//
// Created by rocky on 2022/2/22.
//

#ifndef NDKSTUDY_VIDEOCHANNEL_H
#define NDKSTUDY_VIDEOCHANNEL_H

#include <jni.h>
#include "x264/armeabi-v7a/include/x264.h"
//#include "x264/arm64-v8a/include/x264.h"
#include "librtmp/rtmp.h"
#include <inttypes.h>

class VideoChannel {
    typedef void (*VideoCallback)(RTMPPacket *packet);

public:
    VideoChannel();

    ~VideoChannel();

    //创建x264编码器
    void setVideoEncInfo(int width, int height, int fps, int bitrate);

//真正开始编码一帧数据
    void encodeData(int8_t *data);

    void sendSpsPps(uint8_t *sps, uint8_t *pps, int len, int pps_len);

//发送帧   关键帧 和非关键帧
    void sendFrame(int type, int payload, uint8_t *p_payload);

    void setVideoCallback(VideoCallback callback);

private:
    int mWidth;
    int mHeight;
    int mFps;
    int mBitrate;
//    yuv-->h264 平台 容器 x264_picture_t=bytebuffer
    x264_picture_t *pic_in = 0;
    int ySize;
    int uvSize;
//    编码器
    x264_t *videoCodec = 0;
    VideoCallback callback;//设置数据处理完之后 回调给应用层
};


#endif //NDKSTUDY_VIDEOCHANNEL_H
