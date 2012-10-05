#ARCH_ARM_HAVE_ARMV7A := true
#TARGET_ARCH := arm

# Copyright (c) 2011 The WebRTC project authors. All Rights Reserved.
#
# Use of this source code is governed by a BSD-style license
# that can be found in the LICENSE file in the root of the source
# tree. An additional intellectual property rights grant can be found
# in the file PATENTS.  All contributing project authors may
# be found in the AUTHORS file in the root of the source tree.

ILBC_WRAPPER_MAIN_PATH := $(call my-dir)

include $(ILBC_WRAPPER_MAIN_PATH)/webrtc/src/common_audio/signal_processing/Android.mk
include $(ILBC_WRAPPER_MAIN_PATH)/webrtc/src/modules/audio_coding/codecs/ilbc/Android.mk
include $(ILBC_WRAPPER_MAIN_PATH)/webrtc/src/modules/audio_processing/ns/Android.mk

LOCAL_PATH := $(ILBC_WRAPPER_MAIN_PATH)
include $(CLEAR_VARS)


#LOCAL_ARM_MODE := arm
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES :=\
    iLBC_codec.c

LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/webrtc/src/modules/audio_coding/codecs/ilbc/interface \
    $(LOCAL_PATH)/webrtc/src/modules/audio_coding/codecs/ilbc/ \
    $(LOCAL_PATH)/webrtc/src/common_audio/signal_processing/include \
    $(LOCAL_PATH)/webrtc/src/modules/audio_processing/ns/include/ \
    $(LOCAL_PATH)/webrtc/src/


LOCAL_WHOLE_STATIC_LIBRARIES := \
    libwebrtc_ns \
    libwebrtc_spl \
    libwebrtc_ilbc

LOCAL_MODULE := iLBC_codec

include $(BUILD_SHARED_LIBRARY)
