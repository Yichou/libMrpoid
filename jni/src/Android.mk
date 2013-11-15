LOCAL_PATH := $(call my-dir)


include $(CLEAR_VARS)

LOCAL_MODULE := mrpoid

#LOCAL_STATIC_LIBRARIES := mr_vm_mini
LOCAL_STATIC_LIBRARIES := vm-prebuilt

LOCAL_LDLIBS := -lm -llog -lc -lz -ljnigraphics

LOCAL_C_INCLUDES := $(LOCAL_PATH)/lib

LOCAL_CFLAGS := -DDSM_MINI

LOCAL_SRC_FILES := emulator.c \
	dsm.c \
	network.c \
	utils.c \
	vm.c \
	msgqueue.c \
	register_natives.c \
	encode.c \
	font/tsffont.c \
	lib/TimeUtils.c \
	lib/JniUtils.c

include $(BUILD_SHARED_LIBRARY)
