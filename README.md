# 2023/01/15
- done
  1. Android端开启服务，可以分享文件，ijkplayer可以在线播放分享的文件, 参考文章[搭建Android上的服务器 实现"隔空取物"](https://juejin.cn/post/6844903551408291848)
- bug
  1. FTP服务可以部署，但是ijkplayer对于FTP协议支持有问题，尝试重新编译仍然失败

# 2023/01/01
- done
  1. 切到后台，自动暂停
  2. 循环播放前500集
- bug
  - [x] 前后台切换，黑屏只有声音

# 2022/12/25
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
