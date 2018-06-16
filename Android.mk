LOCAL_PATH := $(call my-dir)

LOCAL_GRADLE_PROJECT_NAME := BlissLauncherO
submodule_path := $(LOCAL_PATH)/$(LOCAL_GRADLE_PROJECT_NAME)
apk_relative_path := app/build/outputs/apk/debug/app-debug.apk
PRIVATE_SRC_FILES := $(LOCAL_GRADLE_PROJECT_NAME)/$(apk_relative_path)

.PHONY: $(submodule_path)/$(apk_relative_path)
$(submodule_path)/$(apk_relative_path) :
        PLATFORM_SDK_VERSION="$(PLATFORM_SDK_VERSION)" PRODUCT_AAPT_PREF_CONFIG="$(PRODUCT_AAPT_PREF_CONFIG)" PRODUCT_LOCALES="$(PRODUCT_LOCALES)" $(submodule_path)/gradlew assembleDebug -p $(submodule_path)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := $(notdir $(LOCAL_PATH))
LOCAL_MODULE_CLASS := APPS
LOCAL_SRC_FILES := $(PRIVATE_SRC_FILES)
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_PRIVILEGED_MODULE := true
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
include $(BUILD_PREBUILT)
include $(CLEAR_VARS)