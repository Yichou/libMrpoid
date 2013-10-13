LOCAL_PATH := $(call my-dir)


include $(CLEAR_VARS)

LOCAL_MODULE := vm-prebuilt
LOCAL_SRC_FILES := libmr_vm_mini.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include

include $(PREBUILT_STATIC_LIBRARY)