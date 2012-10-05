iLBC Android
==========

[Internet low bitrate codec](http://en.wikipedia.org/wiki/Internet_Low_Bit_Rate_Codec). This is a swc that wraps the base functionality of the iLBC codec now maintained by google as part of the [WebRTC](http://www.webrtc.org/) project.

Changes
* Built based on webrtc trunk
* Adds option to use noise supression from webrtc in the encode method
* Uses 30ms mode only

Building
* svn co http://webrtc.googlecode.com/svn/trunk jni/webrtc
* Download the android NDK
* ndk-build NDK_APPLICATION_MK=jni/Applicaiton.mk
