# ARMv7 is significanly faster due to the use of the hardware FPU
APP_STL := gnustl_static
APP_ABI := armeabi armeabi-v7a
APP_OPTIM := release
APP_BUILD_SCRIPT := /Users/lukeweber/iLBC-android-wrapper/jni/Android.mk
# APP_CPPFLAGS += -fexceptions -frtti
