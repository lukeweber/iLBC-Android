/*
 * Copyright (C) 2011 Kyan He <kyan.ql.he@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tuenti.androidilbc;
import android.util.Log;

public class Codec {
    static final private String TAG = "Codec";

    static final private Codec INSTANCE = new Codec();

    /**
     * Encodes PCM 16-bit, 8000hz audio at 30ms iLBC mode.
     * 
     * @param byte[] rawAudio - PCM 16bit 8000hz Audio, ideally in chunks
     * divisible by 480 bytes(240 16 bit samples). Other size will result in
     * audio degredation.
     * @param int offset - Offset in rawAudio
     * @param int length - Length of bytes to read from rawAudio
     * @param byte[] encodedAudio - iLBC encoded audio, 30ms mode.
     * @param int nsMode - Noise Supression Level - 0:Off, 1: Mild, 2: Medium , 3: Aggressive
     * @return int - bytes encoded length.
     */
    public native int encode(byte[] rawAudio, int offset, int length,
            byte[] encodedAudio, int noiseSupressionMode);

    /**
     * Frees and resets the encoder instance
     *
     * After encoding an audio clip this should be called.
     */
    public native int resetEncoder();

    /**
     * Decodes iLBC 30ms mode audio to PCM 16 bit 8000hz audio.
     * 
     * @param byte[] encodedAudio - iLBC 30ms mode encoded audio.
     * @param int offset - Offset in encodedAudio
     * @param int length - Length of bytes to read from encodedAudio.
     * @param byte[] rawAudio - iLBC encoded audio, 30ms mode.
     * @return int - bytes decoded length.
     */
    public native int decode(byte[] encodedAudio, int offset,
            int length, byte[] rawAudio);

    /**
     * Frees and resets the decoder instance
     *
     * After decoding an audio clip this should be called.
     */
    public native int resetDecoder();

    private Codec() {
        System.loadLibrary("iLBC_codec");
    }

    static public Codec instance() {
        return INSTANCE;
    }
}
