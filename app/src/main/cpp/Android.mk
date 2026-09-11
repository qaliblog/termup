LOCAL_PATH:= $(call my-dir)

# Build libtermux-bootstrap
include $(CLEAR_VARS)
LOCAL_MODULE := libtermux-bootstrap
LOCAL_SRC_FILES := termux-bootstrap-zip.S termux-bootstrap.c
include $(BUILD_SHARED_LIBRARY)

# Build libnative-vnc
include $(CLEAR_VARS)
LOCAL_MODULE := native-vnc
LOCAL_SRC_FILES := native-vnc.cpp
LOCAL_C_INCLUDES := $(LOCAL_PATH)
LOCAL_CFLAGS := -std=c++17 -Wall -Wextra -Werror
LOCAL_LDLIBS := -llog -lGLESv2 -ljnigraphics
LOCAL_STATIC_LIBRARIES := libvncclient_static
include $(BUILD_SHARED_LIBRARY)

# Include libvncclient static library
# Note: libvncclient needs to be built separately or included as a prebuilt
# For now, we'll assume it's available as a prebuilt or built as part of the build
$(call import-module,libvncclient)