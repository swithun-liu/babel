# 简介
## 功能

1. 循环播放《名侦探柯南》前500集（数据来自哔哩哔哩）
2. `A(作为主服务器)`&`B`&`C`在同一个局域网内打开此应用, `A`,`B`,`C`可以互相获取对方发送的文本
3. 对接收的文本可以显示翻译
4. nas
   1. `A(作为主服务器)`打开此应用, `B`&`C`可以使用 1. 此应用 或者 2. 其他支持http链接/ftp链接播放视频的应用 播放`A`本地存储的视频(50%)
   2. `A(作为主服务器)`打开此应用, B&C可以使用本应用下载`A`中文件/上传文件到`A`(30%)

## 文件目录
- **backend**
  - phone-monitor-backend: rust写的服务端，作为服务器(废弃)
  - rust-server-android-lib: **rust写的服务端**，然后打包成so给Android用
- **frontend**
  - swithunApp：**主角**
- **myDependencyBuild**: 需要自己编译的一些第三方依赖
  - myIjkplayer：app使用的视频播放器
- myPracticeDemo：测试一些方案

# 进展

# 2023/03/12
- done
  1. 搜索局域网内启动的server，目前使用ping过滤有LAN内设备ip，太慢需要优化
  2. 界面布局调整

# 2023/03/05
- done
  1. 支持播放server的视频，随意跳转
     实现跳转: Content-Range

# 2023/02/26
- done
  1. client `-http request->` server `-websocket->` android `-websocket->` server `-http response->` client

## 2023/02/19
- done
  1. 将rust服务端成功打包成so，Android app可作为服务端

## 2023/01/15
- done
  1. Android端开启服务，可以分享文件，ijkplayer可以在线播放分享的文件, 参考文章[搭建Android上的服务器 实现"隔空取物"](https://juejin.cn/post/6844903551408291848)
- bug
  - [ ] FTP协议：FTP服务可以部署，但是ijkplayer对于FTP协议支持有问题，尝试重新编译仍然失败，目前改成使用HTTP协议获取视频文件
  - [ ] HTTP协议：Android作为服务端，给服务端上传文件，如何获取还有问题，目前先使用FTP上传文件
  - [ ] mkv电影播放卡顿/卡住，不确定是大文件造成(10+GB)还是MVK文件造成
- todo
  - [ ] 文件列表展示

## 2023/01/01
- done
  1. 切到后台，自动暂停
  2. 循环播放前500集
- bug
  - [x] 前后台切换，黑屏只有声音

## 2022/12/25
- done
  1. 能接收电脑复制的文本，app端显示翻译
  2. 可以播放番剧
- bug
  - [x] surfaceView保存在静态变量导致内存泄漏
  - [x] 播放完成会crash
- 坑
  1. ijkplayer编译失败
  2. ijkplayer需要使用AndroidView: SurfaceView，参考[文章](https://www.jianshu.com/p/5aa224d1ec83)
  3. ijkplayer防盗链403，参考[文章](https://blog.csdn.net/xiaoduzi1991/article/details/121968386)

# 感谢
- [bilibili api](https://github.com/SocialSisterYi/bilibili-API-collect/tree/master/login/login_action)
