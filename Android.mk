LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_PACKAGE_NAME := BlissLauncherO
LOCAL_CERTIFICATE := platform
LOCAL_DEX_PREOPT := false

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_MANIFEST_FILE := app/src/main/AndroidManifest.xml
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/app/src/main/res/
ifeq ($(TARGET_BUILD_APPS),)
    LOCAL_RESOURCE_DIR += frameworks/support/v7/appcompat/res
    LOCAL_RESOURCE_DIR += frameworks/support/v7/gridlayout/res
else
    LOCAL_RESOURCE_DIR += prebuilts/sdk/current/support/v7/appcompat/res
    LOCAL_RESOURCE_DIR += prebuilts/sdk/current/support/v7/gridlayout/res
endif
LOCAL_SRC_FILES := $(call all-java-files-under, app/src/main/)

# unbundled
LOCAL_STATIC_JAVA_LIBRARIES := \
        android-common \
        android-support-v4 \
        android-support-v7-appcompat \
        android-support-v7-gridlayout \
        rxrelay \
        rxjava \
        rxandroid 


LOCAL_SDK_VERSION := current

LOCAL_PRIVILEGED_MODULE := true

LOCAL_AAPT_FLAGS := --auto-add-overlay \
--extra-packages android.support.v4 \
--extra-packages android.support.v7.appcompat \
--extra-packages android.support.v7.gridlayout \


include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := rxrelay:libs/rxrelay-2.0.0.jar rxjava:libs/rxjava-2.1.10.jar  rxandroid:libs/rxandroid-2.0.2.aar

include $(BUILD_MULTI_PREBUILT)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))

