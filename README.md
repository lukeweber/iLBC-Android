iLBC Android
==========

[Internet low bitrate codec](http://en.wikipedia.org/wiki/Internet_Low_Bit_Rate_Codec). This is a swc that wraps the base functionality of the iLBC codec now maintained by google as part of the [WebRTC](http://www.webrtc.org/) project.

Based loosely on an android version of iLBC at http://code.google.com/p/android-ilbc.

Changes
* Built based on webrtc code instead of rfc
* Adds option to use noise supression from webrtc in the encode method.
* Uses 30ms mode only.

Building
* Download webrtc into jni directory. svn co http://webrtc.googlecode.com/svn/trunk jni/webrtc
* Download the android NDK and run ndk-build in the root directory.
