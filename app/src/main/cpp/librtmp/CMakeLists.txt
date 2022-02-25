 # 关闭ssl 不支持rtmps
 set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPTO")
 #设置全局变量
 file(GLOB rtmp_source *.c)
 add_library(
         rtmp
         STATIC
         ${rtmp_source}
 )