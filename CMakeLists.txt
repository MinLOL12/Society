cmake_minimum_required(VERSION 3.21)
set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
if ("${CMAKE_SYSTEM_NAME}" STREQUAL "iOS" OR IOS)
    set(CMAKE_OSX_ARCHITECTURES "arm64")
else()
    set(CMAKE_OSX_ARCHITECTURES "arm64;x86_64")
endif()
set(CMAKE_CXX_VISIBILITY_PRESET hidden)

project(ProgressiveStats VERSION 1.0.0)

file(GLOB_RECURSE SOURCES CONFIGURE_DEPENDS src/*.cpp)

add_library(${PROJECT_NAME} SHARED ${SOURCES})

# The Geode CLI normally provides this through GEODE_SDK.  Keep a CMake cache
# option as well: environment variables set in one PowerShell window are not
# automatically available in another one, and -D paths work reliably with
# Windows paths containing spaces.
set(GEODE_SDK "" CACHE PATH "Path to the Geode SDK")

set(GEODE_SDK_PATH "")
if (GEODE_SDK)
    set(GEODE_SDK_PATH "${GEODE_SDK}")
elseif (DEFINED ENV{GEODE_SDK} AND NOT "$ENV{GEODE_SDK}" STREQUAL "")
    set(GEODE_SDK_PATH "$ENV{GEODE_SDK}")
endif()

if (NOT GEODE_SDK_PATH OR NOT EXISTS "${GEODE_SDK_PATH}/CMakeLists.txt")
    message(FATAL_ERROR
        "Geode SDK was not found. Set the GEODE_SDK environment variable, or "
        "configure with -DGEODE_SDK=<path-to-Geode-SDK>.\n"
        "PowerShell example:\n"
        "  $env:GEODE_SDK = 'C:\\path\\to\\geode'\n"
        "  cmake -B build -DCMAKE_BUILD_TYPE=RelWithDebInfo\n"
        "The SDK directory must contain CMakeLists.txt.")
endif()

get_filename_component(GEODE_SDK_PATH "${GEODE_SDK_PATH}" REALPATH)
message(STATUS "Found Geode SDK: ${GEODE_SDK_PATH}")
add_subdirectory("${GEODE_SDK_PATH}" "${CMAKE_CURRENT_BINARY_DIR}/geode")

setup_geode_mod(${PROJECT_NAME})
