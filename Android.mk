LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# Module_Tags controls what build flavors the package gets included in.
LOCAL_MODULE_TAGS := optional

# Package name of the app.
LOCAL_PACKAGE_NAME := BlissLauncher

# Certificate to use to sign the apk. In this case, sign using platform certificate.
LOCAL_CERTIFICATE := platform
LOCAL_DEX_PREOPT := false

# Enable or disable proguard.
LOCAL_PROGUARD_ENABLED := disabled

# Manifest file for this app.
LOCAL_MANIFEST_FILE := app/src/main/AndroidManifest.xml

# Resource directory for this app.
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/app/src/main/res/
ifeq ($(TARGET_BUILD_APPS),)
    LOCAL_RESOURCE_DIR += frameworks/support/v7/appcompat/res
    LOCAL_RESOURCE_DIR += frameworks/support/v7/gridlayout/res
else
    LOCAL_RESOURCE_DIR += prebuilts/sdk/current/support/v7/appcompat/res
    LOCAL_RESOURCE_DIR += prebuilts/sdk/current/support/v7/gridlayout/res
endif

# Java source directory for this app.
LOCAL_SRC_FILES := $(call all-java-files-under, app/src/main/)

# Unbundled java libraries.
LOCAL_STATIC_JAVA_LIBRARIES := \
        android-common \
        android-support-v4 \
        android-support-v7-appcompat \
        android-support-v7-gridlayout \
        rxrelay \
        rxjava

# Unbundled aar libraries.
LOCAL_STATIC_JAVA_AAR_LIBRARIES := rxandroid \
        calligraphy \
        circleindicator

# SDK Version for this build.
LOCAL_SDK_VERSION := current

LOCAL_PRIVILEGED_MODULE := true

# Flags for AAPT.
LOCAL_AAPT_FLAGS := --auto-add-overlay \
--extra-packages android.support.v4 \
--extra-packages android.support.v7.appcompat \
--extra-packages android.support.v7.gridlayout \
--extra-packages uk.co.chrisjenx.calligraphy \
--extra-packages me.relex.circleindicator

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

# Prebuilt java and aar libraries.
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := rxrelay:libs/rxrelay-2.0.0.jar \
        rxjava:libs/rxjava-2.1.10.jar \
        rxandroid:libs/rxandroid-2.0.2.aar \
        circleindicator:libs/circleindicator-1.2.2.aar \
        calligraphy:libs/calligraphy-2.3.0.aar

include $(BUILD_MULTI_PREBUILT)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
