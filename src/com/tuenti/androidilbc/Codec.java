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
    public native int encode(byte[] data, int dataOffset, int dataLength,
            byte[] samples, int nsMode);

    public native int resetEncoder();

    public native int decode(byte[] samples, int samplesOffset,
            int samplesLength, byte[] data);

    public native int resetDecoder();

    private Codec() {
        System.loadLibrary("iLBC_codec");
    }

    static public Codec instance() {
        return INSTANCE;
    }

    /*
     * 
     * @param nsMode - 0:Off, 1: Mild, 2: Medium , 3: Aggressive
     */
    public byte[] encode(byte[] samples, int offset, int len, int nsMode) {
        byte[] data = new byte[len / 480 * 50 ];//Each frame is 240 shorts, or 480 bytes, which is compressed to 50 bytes
        int bytesEncoded = 0;
        bytesEncoded += encode(samples, offset, len, data, 0);
        Log.e(TAG, "Encode " + bytesEncoded);
        return data;
    }

    public byte[] decode(byte[] data, int offset, int len) {
        return null;
    }
}
