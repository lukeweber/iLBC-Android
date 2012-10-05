iLBC Android
==========

[Internet low bitrate codec](http://en.wikipedia.org/wiki/Internet_Low_Bit_Rate_Codec). Wrapper and shared objects to use ilbc in android.

ilbc quirks
* Call resetEncoder/Decoder between encoding or decoding entirely new audio clicks, not in between encoding chunks of a stream, if you get this wrong you'll hear clicking. Read next point for technical reason.
* Encoded audio from ilbc starts with a block of zeros. See http://www.ietf.org/rfc/rfc3951.txt, Specifically: "The input to the LPC analysis module is a possibly high-pass filtered speech buffer, speech_hp, that contains 240/300 (LPC_LOOKBACK + BLOCKL = 80/60 + 160/240 = 240/300) speech samples, where samples 0 through 79/59 are from the previous block and samples 80/60 through 239/299 are from the current block.  No look-ahead into the next block is used.  For the very first block processed, the look-back samples are assumed to be zeros."

Changes
* Built based on webrtc trunk
* Adds option to use noise supression from webrtc in the encode method
* Uses 30ms mode only

Building
* svn co http://webrtc.googlecode.com/svn/trunk jni/webrtc
* Download the android NDK
* ndk-build NDK_APPLICATION_MK=jni/Applicaiton.mk
