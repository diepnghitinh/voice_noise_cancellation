cmake_minimum_required(VERSION 3.4.1)

# convert SDK path to forward slashes on Windows
file(TO_CMAKE_PATH ${PATH_TO_SUPERPOWERED} PATH_TO_SUPERPOWERED)
file(GLOB source_files
    "src/main/cpp/*.h"
    "src/main/cpp/*.c"

    "src/main/cpp/VAD/*.h"
    "src/main/cpp/VAD/*.c"

    "src/main/cpp/VAD/FeatureExtraction/*.h"
    "src/main/cpp/VAD/FeatureExtraction/*.c"

    "src/main/cpp/VAD/RandomForest1VAD/*.h"
    "src/main/cpp/VAD/RandomForest1VAD/*.c"

    "src/main/cpp/NoiseReductionCode/*.h"
    "src/main/cpp/NoiseReductionCode/*.c"

    "src/main/cpp/DynamicRangeMultibandCompression/*.h"
    "src/main/cpp/DynamicRangeMultibandCompression/*.c"

    "src/main/cpp/CommonHeaders/*.h"
    "src/main/cpp/CommonHeaders/*.c"
)

include_directories(src/main/cpp)
include_directories(${PATH_TO_SUPERPOWERED})

# compile player example to a native library
add_library (
        FrequencyDomain
        SHARED
        ${source_files}
        src/main/cpp/FrequencyDomain.cpp
        ${PATH_TO_SUPERPOWERED}/AndroidIO/SuperpoweredAndroidAudioIO.cpp
)

# link the native library against the following libraries
target_link_libraries (
        FrequencyDomain
        log
        android
        OpenSLES
        ${PATH_TO_SUPERPOWERED}/libSuperpoweredAndroid${ANDROID_ABI}.a
)
