# 简介
babel/蓝田
## 功能

**前置操作**:  `A`, `B`, `C`同时打开应用，`A`启动内核服务，`A` `B` `C` 连接 `A` 内核即可加入会话
1. 文件传输助手  
  可以在会话中发送文件/文字，发送的文件点击可下载
2. nas
  可以访问 `A` 中的文件  
  - [x] 视频文件点击即可在应用内播放
  - [ ] 文件下载
  - [ ] 文件上传
3. 工作/学习陪伴
  - [x] 循环播放《名侦探柯南》前500集（自用）
  - [ ] 支持自定义数据源

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

## 2023/07/15
- done
  1. 解决自动下一集画面不会自动更新

## 2023/04/08
- done
  1. 会话可以发送文件，下载会话文件
  2. 整理Android代码，迁移MVI架构，View和Viewmodel间已经实现单向数据流，但是viewmodel间交互还需要思考一下，目前是通过public方法，这样会违反单向数据流的设计原则，考虑通过Action实现，在Action中添加callback，我在思考这样是否违背原则，我倾向于不违反，reduce作为对viewmodel的数据，uiSateFlow作为viewmodel的输出，前者理解为request，后者理解为response，在Action中添加callback也可以理解为response，但是这样感觉不太收敛。

## 2023/04/02
- done
  1. 成功使用websocket分片发送文件到服务器，服务器保存文件，但是后续需要增加出错重传的处理。

## 2023/03/25
- done
  1. 升级`compose`依赖之后，`AndroidView`导致其他`view`黑屏。
  2. 并发`ping LAN`，加速局域网内`server`查找。

## 2023/03/12
- done
  1. 搜索局域网内启动的server，目前使用ping过滤有LAN内设备ip，太慢需要优化
  2. 界面布局调整

## 2023/03/05
- done
  1. 支持播放server的视频，随意跳转
     实现跳转: Content-Range

## 2023/02/26
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
