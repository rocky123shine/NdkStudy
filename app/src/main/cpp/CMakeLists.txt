
cmake_minimum_required(VERSION 3.10.2)
project("ndkstudy")
#引入资源文件夹
add_subdirectory(librtmp)

# 静态文件使用
# 引入头文件 如文件在CMakeList统计目录下的x264中 ANDROID_ABI(系统定义的变量)
include_directories(${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/include)
#引入静态库
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/lib")
file(GLOB cpp_source *.cpp)
add_library(
        ndkstudy
        SHARED
        ${cpp_source}
)

find_library(
        log-lib
        log)


target_link_libraries(
        ndkstudy
        ${log-lib}
        rtmp
        x264 # 链接x264  就是libx264.a 系统自动加上lib前缀
)