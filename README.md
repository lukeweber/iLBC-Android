iLBC Android
==========

[Internet low bitrate codec](http://en.wikipedia.org/wiki/Internet_Low_Bit_Rate_Codec). This is a swc that wraps the base functionality of the iLBC codec now maintained by google as part of the [WebRTC](http://www.webrtc.org/) project.

Based loosely on an android version of iLBC at http://code.google.com/p/android-ilbc.

Changes
* Built based on webrtc code instead of rfc
* Adds option to use noise supression from webrtc in the encode method.

Codec.java included in this project should be compatible with the Demo.java found at http://code.google.com/p/android-ilbc/source/browse/src/com/googlecode/androidilbc/Demo.java except for the package name. As well to make that demo work, you would have to update the AndroidManifest.xml for your purposes.
