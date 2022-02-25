#include <jni.h>
#include <string>
#include <pthread.h>
#include "VideoChannel.h"
#include "maniulog.h"
#include "safe_queue.h"

extern "C" {
#include "librtmp/rtmp.h"
}
VideoChannel *videoChannel = 0;
int isStart = 0;
//记录子线程对象
pthread_t pid;
RTMP *rtmp = 0;
//推流标志位
int readyPushing = 0;
uint32_t start_time;
//阻塞式队列
SafeQueue<RTMPPacket *> packets;

void videoCallback(RTMPPacket *packet) {

    if (packet) {
        if (packets.size() > 50) {
            packets.clear();
        }
        packet->m_nTimeStamp = RTMP_GetTime() - start_time;
        packets.push(packet);
    }

}


void releasePackets(RTMPPacket *&packet) {
    if (packet) {
        RTMPPacket_Free(packet);
        delete packet;
        packet = 0;
    }
}

void *startRtmp(void *args) {
    char *url = static_cast<char *>(args);
    do {
        //创建rtmp的引用
        rtmp = RTMP_Alloc();
        if (!rtmp) {
            LOGE("rtmp 创建失败");
            break;
        }
        //初始化rtmp
        RTMP_Init(rtmp);
        //设置超时时间
        rtmp->Link.timeout = 5;
        //设置链接地址
        int ret = RTMP_SetupURL(rtmp, url);
        if (!ret) {
            LOGE("rtmp设置地址失败:%s", url);
            break;
        }
        //开启输出模式
        RTMP_EnableWrite(rtmp);
        //链接
        ret = RTMP_Connect(rtmp, 0);
        if (!ret) {
            LOGE("rtmp连接地址失败:%s", url);
            break;
        }
        ret = RTMP_ConnectStream(rtmp, 0);

        if (!ret) {
            LOGE("rtmp连接流失败:%s", url);
            break;
        }
        LOGE("rtmp连接成功----------->:%s", url);
        //准备就绪 开始推流
        readyPushing = 1;
        //记录一个开始推流的时间
        start_time = RTMP_GetTime();
        packets.setWork(1);
        RTMPPacket *packet = 0;
        while (isStart) {
            packets.pop(packet);
            if (!isStart) {
                break;
            }
            if (!packet) {
                continue;
            }
            // 给rtmp的流id
            packet->m_nInfoField2 = rtmp->m_stream_id;
            //发送包 1:加入队列发送
            ret = RTMP_SendPacket(rtmp, packet, 1);
            releasePackets(packet);
            if (!ret) {
                LOGE("发送数据失败");
                break;
            }
        }
        releasePackets(packet);

    } while (0);


    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
    }
    delete url;
    return 0;

}


extern "C" JNIEXPORT jstring JNICALL
Java_com_rocky_ndkstudy_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rocky_ndkstudy_live_LivePusher_native_1setVideoEncInfo(JNIEnv *env, jobject thiz,
                                                                jint height, jint width, jint fps,
                                                                jint bitrate) {
    if (videoChannel) {
        videoChannel->setVideoEncInfo(width, height, fps, bitrate);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_rocky_ndkstudy_live_LivePusher_native_1pushVideo(JNIEnv *env, jobject thiz,
                                                          jbyteArray _data) {
//    data  yuv 1   h264  2
//没有链接 成功
    if (!videoChannel || !readyPushing) {
        return;
    }

    jbyte *data = env->GetByteArrayElements(_data, NULL);

    //开始编码
    videoChannel->encodeData(data);
    env->ReleaseByteArrayElements(_data, data, 0);


}

extern "C"
JNIEXPORT void JNICALL
Java_com_rocky_ndkstudy_live_LivePusher_native_1init(JNIEnv *env, jobject thiz) {
    videoChannel = new VideoChannel;
    videoChannel->setVideoCallback(videoCallback);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_rocky_ndkstudy_live_LivePusher_native_1release(JNIEnv *env, jobject thiz) {

}
extern "C"
JNIEXPORT void JNICALL
Java_com_rocky_ndkstudy_live_LivePusher_native_1start(JNIEnv *env, jobject thiz,
                                                      jstring _path) {
    //子线程 创建rtmp服务器
    if (isStart) return;
    const char *path = env->GetStringUTFChars(_path, 0);
    char *url = new char[strlen(path) + 1];
    strcpy(url, path);
    //直播开始
    isStart = 1;
    //创建子线程
    //参数1.要创建的线程引用
    //参数3.子线程要执行的函数指针
    //参数4.函数指针需要的参数
    pthread_create(&pid, 0, startRtmp, url);
    env->ReleaseStringUTFChars(_path, path);

}