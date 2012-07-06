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
                jbyteArray src, jint src_offset, jint src_len, jbyteArray dest, jshort frame_length_ms, jint ns_mode)
{
    jsize src_size, dest_size;
    jbyte *src_bytes, *dest_bytes;

    int bytes_remaining, bytes_encoded, bytes;
    short frame_samples, frame_size_bytes;

    encoding = 1;
    if(Enc_Inst == NULL){
        WebRtcIlbcfix_EncoderCreate(&Enc_Inst);
        WebRtcIlbcfix_EncoderInit(Enc_Inst, frame_length_ms);
    }

    frame_samples = 8 * frame_length_ms;
    frame_size_bytes = frame_samples * sizeof(short);
    src_size = (*env)->GetArrayLength(env, src);
    src_bytes = (*env)->GetByteArrayElements(env, src, JNI_COPY);
    dest_size = (*env)->GetArrayLength(env, dest);
    dest_bytes = (*env)->GetByteArrayElements(env, dest, JNI_COPY);

    src_bytes += src_offset;
    bytes_remaining = src_len;

    int truncated = bytes_remaining % frame_size_bytes;
    if(truncated){
        bytes_remaining -= truncated;
        LOGW("Ignoring last %d bytes, input must be divisible by %d", truncated, frame_size_bytes);
    }

    if(ns_mode < 0 || ns_mode > 2){
        LOGE("Noise supression must be set to a value of 0-3, 0:Off, 1: Mild, 2: Medium , 3: Aggressive");
        return -1;
    }

    if(ns_mode > 0){
        noise_supression((short*)src_bytes, bytes_remaining, ns_mode -1);
    }

    while(bytes_remaining > 0){
        bytes = WebRtcIlbcfix_Encode(Enc_Inst, (short* )src_bytes, frame_samples, (WebRtc_Word16 *)dest_bytes);
        src_bytes += frame_size_bytes;
        bytes_encoded += frame_size_bytes;
        bytes_remaining -= frame_size_bytes;

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
            jbyteArray src, jint src_offset, jint src_len, jshort jframe_length_ms, jbyteArray dest)
{
    jsize src_size, dest_size;
    jbyte *src_bytes, *dest_bytes;

    int bytes_remaining, bytes_decoded, num_samples;
    short speechType;
    short frame_length_ms = jframe_length_ms;

    src_size = (*env)->GetArrayLength(env, src);
    src_bytes = (*env)->GetByteArrayElements(env, src, JNI_COPY);
    dest_size = (*env)->GetArrayLength(env, dest);
    dest_bytes = (*env)->GetByteArrayElements(env, dest, JNI_COPY);

    decoding = 1;
    if(Dec_Inst == NULL){
        WebRtcIlbcfix_DecoderCreate(&Dec_Inst);
        WebRtcIlbcfix_DecoderInit(Dec_Inst, frame_length_ms);
    }

    int bytes_per_frame;
    if (frame_length_ms == 20) {
      bytes_per_frame = 38;
    } else if (frame_length_ms == 30) {
      bytes_per_frame = 50;
    } else {
      LOGE("Frame size must be 20 or 30 ms.");
      return -1;
    }

    src_bytes += src_offset;
    bytes_remaining = src_len;
    bytes_decoded = 0;

    int i = 0;
    while(bytes_remaining > 0){
        num_samples = WebRtcIlbcfix_Decode(Dec_Inst, (short *)src_bytes, bytes_per_frame, (short *)dest_bytes, &speechType);
        src_bytes += bytes_per_frame;
        bytes_remaining -= bytes_per_frame;
        bytes_decoded += bytes_per_frame;
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
