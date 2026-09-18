cmake_minimum_required(VERSION 3.18)

project(TermUpNative C CXX ASM)

# Make sure we use the correct C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Include directories
include_directories(${CMAKE_CURRENT_SOURCE_DIR})

# Build libtermux-bootstrap (embeds the per-ABI bootstrap zip via .incbin and
# provides Java_com_termux_app_TermuxInstaller_getZip for TermuxInstaller)
add_library(termux-bootstrap SHARED
    termux-bootstrap-zip.S
    termux-bootstrap.c
)

# The .S file uses .incbin with paths relative to this directory, so tell the
# assembler where to find the bootstrap zips.
set_source_files_properties(termux-bootstrap-zip.S PROPERTIES
    COMPILE_OPTIONS "-Wa,-I${CMAKE_CURRENT_SOURCE_DIR}"
)

# Build libnative-vnc
add_library(native-vnc SHARED
    native-vnc.cpp
)

# Link against libvncclient (prebuilt, placed in jniLibs by prepareTermUpAssets)
target_link_libraries(native-vnc
    ${CMAKE_CURRENT_SOURCE_DIR}/../../jniLibs/${ANDROID_ABI}/libvncclient.a
    log
    GLESv2
    jnigraphics
    z
)

# Add include directories for libvncclient
target_include_directories(native-vnc PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/libvncserver_include
)