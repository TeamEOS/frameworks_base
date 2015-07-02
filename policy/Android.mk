LOCAL_PATH:= $(call my-dir)

# the library
# ============================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_JAVA_LIBRARIES := org.teameos.navigation
LOCAL_REQUIRED_MODULES := org.teameos.navigation
LOCAL_MODULE := android.policy
LOCAL_STATIC_JAVA_LIBRARIES := \
    org.cyanogenmod.platform.sdk \
    org.teameos.navigation-static

include $(BUILD_JAVA_LIBRARY)

# additionally, build unit tests in a separate .apk
include $(call all-makefiles-under,$(LOCAL_PATH))
