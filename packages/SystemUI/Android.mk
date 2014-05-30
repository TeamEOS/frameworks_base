LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
    src/com/android/systemui/EventLogTags.logtags

LOCAL_JAVA_LIBRARIES := telephony-common

LOCAL_STATIC_JAVA_LIBRARIES := \
    gdx-static

LOCAL_PACKAGE_NAME := SystemUI
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

LIB_GDX_STUB := ../../../../external/libgdx
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := gdx-static:$(LIB_GDX_STUB)/gdx.jar

include $(BUILD_MULTI_PREBUILT)

include $(call all-makefiles-under,$(LOCAL_PATH))


