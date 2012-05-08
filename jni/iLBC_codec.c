#include "jni.h"
#include <math.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "ilbc.h"
#include "noise_suppression_x.h"
#include "defines.h"

#define LOG_TAG "iLBC_codec"

#include <android/log.h>
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__) 
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , LOG_TAG, __VA_ARGS__) 
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , LOG_TAG, __VA_ARGS__) 
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , LOG_TAG, __VA_ARGS__) 
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , LOG_TAG, __VA_ARGS__)

#define JNI_COPY  0

static iLBC_encinst_t *Enc_Inst = NULL;
static iLBC_decinst_t *Dec_Inst = NULL;
static NsxHandle *nsxInst = NULL;

const short MODE = 30;//30ms
const short FRAME_SAMPLES = 240;//MODE << 3;//240
const short FRAME_SIZE = 480;//FRAME_SAMPLES * sizeof(short);//480

static int encoding, decoding, encoderReset, decoderReset;

static void noise_supression(short* a, int samples, int ns_mode){
    short tmp_input[80];
    if(nsxInst == NULL){
        WebRtcNsx_Create(&nsxInst);
        WebRtcNsx_Init(nsxInst, 8000);//8000 hz sampling
        WebRtcNsx_set_policy(nsxInst, ns_mode);//0: Mild, 1: Medium , 2: Aggressive
    }
    int i;
    for (i = 0; i < samples; i+= 80){
        memcpy(tmp_input, &a[i], 80 * sizeof(short));
        WebRtcNsx_Process(nsxInst, tmp_input, NULL, (short *)&a[i], NULL);
    }
}

static void reset_encoder_impl(){
    if(!encoding){
        WebRtcIlbcfix_EncoderFree(Enc_Inst);
        Enc_Inst = NULL;
        WebRtcNsx_Free(nsxInst);
        nsxInst = NULL;
        encoderReset = 0;
    } else {
        encoderReset = 1;
    }
}

jint Java_com_tuenti_androidilbc_Codec_resetEncoder(JNIEnv *env, jobject this){
    reset_encoder_impl();
    return 1;
}

jint Java_com_tuenti_androidilbc_Codec_encode(JNIEnv *env, jobject this,
                jbyteArray src, jint src_offset, jint src_len, jbyteArray dest, jint ns_mode)
{
    jsize src_size, dest_size;
    jbyte *src_bytes, *dest_bytes;

    int bytes_remaining, bytes_encoded, bytes;

    encoding = 1;
    if(Enc_Inst == NULL){
        WebRtcIlbcfix_EncoderCreate(&Enc_Inst);
        WebRtcIlbcfix_EncoderInit(Enc_Inst, MODE);
    }

    src_size = (*env)->GetArrayLength(env, src);
    src_bytes = (*env)->GetByteArrayElements(env, src, JNI_COPY);
    dest_size = (*env)->GetArrayLength(env, dest);
    dest_bytes = (*env)->GetByteArrayElements(env, dest, JNI_COPY);

    src_bytes += src_offset;
    bytes_remaining = src_len;

    int truncated = bytes_remaining % FRAME_SIZE;
    if(truncated){
        bytes_remaining -= truncated;
        LOGW("Ignoring last %d bytes, input must be divisible by %d", truncated, FRAME_SIZE);
    }

    if(ns_mode < 0 || ns_mode > 2){
        LOGE("Noise supression must be set to a value of 0-3, 0:Off, 1: Mild, 2: Medium , 3: Aggressive");
        return -1;
    }

    if(ns_mode > 0){
        noise_supression((short*)src_bytes, bytes_remaining, ns_mode -1);
    }

    while(bytes_remaining > 0){
        bytes = WebRtcIlbcfix_Encode(Enc_Inst, (short* )src_bytes, FRAME_SAMPLES, (WebRtc_Word16 *)dest_bytes);
        src_bytes += FRAME_SIZE;
        bytes_encoded += FRAME_SIZE;
        bytes_remaining -= FRAME_SIZE;

        dest_bytes += bytes;
    }
    src_bytes -= bytes_encoded;
    dest_bytes -= src_len;

    (*env)->ReleaseByteArrayElements(env, src, src_bytes, JNI_COPY);
    (*env)->ReleaseByteArrayElements(env, dest, dest_bytes, JNI_COPY);

    encoding = 0;
    if(encoderReset){
        reset_encoder_impl();
    }

    return bytes_encoded;
}

static void reset_decoder_impl(){
    if(!decoding){
        WebRtcIlbcfix_DecoderFree(Dec_Inst);
        Dec_Inst = NULL;
        decoderReset = 0;
    } else {
        decoderReset = 1;
    }
}

jint Java_com_tuenti_androidilbc_Codec_resetDecoder(JNIEnv *env, jobject this){
    reset_decoder_impl();
    return 1;
}

jint Java_com_tuenti_androidilbc_Codec_decode(JNIEnv *env, jobject this, 
            jbyteArray src, jint src_offset, jint src_len, jbyteArray dest)
{
    jsize src_size, dest_size;
    jbyte *src_bytes, *dest_bytes;

    int bytes_remaining, bytes_decoded, num_samples;
    short speechType;

    src_size = (*env)->GetArrayLength(env, src);
    src_bytes = (*env)->GetByteArrayElements(env, src, JNI_COPY);
    dest_size = (*env)->GetArrayLength(env, dest);
    dest_bytes = (*env)->GetByteArrayElements(env, dest, JNI_COPY);

    decoding = 1;
    if(Dec_Inst == NULL){
        WebRtcIlbcfix_DecoderCreate(&Dec_Inst);
        WebRtcIlbcfix_DecoderInit(Dec_Inst, MODE);
    }

    src_bytes += src_offset;
    bytes_remaining = src_len;

    int i = 0;
    while(bytes_remaining > 0){
        num_samples = WebRtcIlbcfix_Decode(Dec_Inst, (short *)src_bytes, NO_OF_BYTES_30MS, (short *)dest_bytes, &speechType);
        src_bytes += NO_OF_BYTES_30MS;
        bytes_remaining -= NO_OF_BYTES_30MS;
        bytes_decoded += NO_OF_BYTES_30MS;
        dest_bytes += num_samples * sizeof(short);
    }

    src_bytes -= bytes_decoded;
    dest_bytes -= src_len;

    (*env)->ReleaseByteArrayElements(env, src, src_bytes, JNI_COPY);
    (*env)->ReleaseByteArrayElements(env, dest, dest_bytes, JNI_COPY);

    decoding = 0;
    if(decoderReset){
        reset_decoder_impl();
    }

    return bytes_decoded;
}
