# ImageShare - 基于 Android 与 Flask 的局域网图片分享系统

##  项目简介
ImageShare 是一款基于 C/S（客户端/服务器）架构的轻量级局域网图片分享应用。系统由 **Android 客户端** 和 **Python Flask 后端** 组成，旨在解决无外网或企业内部局域网环境下的图片定向传输需求。用户可以通过 Android 端注册登录、获取在线用户列表、选择图片进行定向发送；接收端可以拉取待接收图片，并一键保存至本地系统相册。后端提供完整的 RESTful API 并实现了服务器关闭时的垃圾资源回收机制。

##  功能清单
**客户端（Android）**：
- **用户认证**：注册、登录及退出登录，基于 SharedPreferences 实现用户状态持久化。
- **动态权限申请**：完美适配 Android 13+ (READ_MEDIA_IMAGES) 及低版本 (READ_EXTERNAL_STORAGE) 存储权限。
- **图片发送**：获取在线用户列表，选择目标接收人，从系统相册选图并上传至服务器。
- **图片接收**：轮询拉取当前用户的待接收图片，异步下载并在 UI 线程展示，支持一键保存至系统相册并通知服务器标记为已接收。
- **UI/UX**：统一的渐变背景主题，自定义按钮、输入框及列表样式，提升交互体验。

**服务端（Flask）**：
- **RESTful API**：提供用户注册、登录、用户列表获取、图片上传、待接收图片拉取、标记已接收等接口。
- **数据持久化**：使用 SQLite 存储用户信息与图片分享记录。
- **资源清理机制**：利用 `atexit` 注册钩子，在服务器正常关闭时自动清理未接收的图片文件及数据库记录，防止磁盘空间泄漏。

##  技术栈
- **客户端**：Java, XML Layout, Material Components, Retrofit2 + OkHttp3 (登录/注册/上传), Volley (列表/接收), Gson, ExecutorService, SharedPreferences。
- **服务端**：Python, Flask, SQLite, RESTful API, uuid, atexit。
- **构建与部署**：Gradle (KTS), PyCharm, Android Studio。

##  系统架构图
`
[Android 客户端] 
  ├─ UI 层: Activity (Login, Main, UserList, ReceiveImage)
  ├─ 网络层: Retrofit2 / Volley (HTTP/1.1 传输)
  └─ 数据层: SharedPreferences, 本地文件存储
       ↓ ↑ (RESTful API over TCP)
[Flask 后端服务]
  ├─ 路由层: Flask Router (/login, /upload_image, /get_pending_images 等)
  ├─ 业务层: 权限校验、ID 格式校验、文件流处理
  └─ 数据层: SQLite (users.db) + 本地磁盘 (uploads/)
`

## 项目结构
`
ImageShare/
├── android-app/                  # Android 客户端代码
│   ├── app/src/main/java/com/example/imageshare/
│   │   ├── activities/           # UI 页面与逻辑
│   │   ├── api/                  # Retrofit 配置与接口定义
│   │   └── models/               # 数据实体类
│   └── build.gradle.kts
├── flask-backend/                # Python Flask 服务端代码
│   ├── app.py                    # 后端核心逻辑
│   ├── users.db                  # SQLite 数据库 (自动生成)
│   └── uploads/                  # 图片上传目录 (自动生成)
└── README.md
`

## 快速开始
1. 启动后端服务
   确保电脑已安装 Python 环境。
    进入 flask-backend 目录，安装依赖：
    `pip install flask`
   启动服务器（服务器将运行在 0.0.0.0:5000）：
    `python app.py`
2. 配置并运行 Android 客户端
   使用 Android Studio 打开 android-app 项目。
   打开 `app/src/main/java/com/example/imageshare/api/ApiClient.java`，将 BASE_URL 修改为你运行 Flask 服务端电脑的局域网 IP 地址（例如：http://192.168.1.100:5000/）。
   确保手机和电脑连接在同一个 Wi-Fi 或局域网下。
   编译并安装 App 到真机或模拟器。

## 后端 API 接口说明
POST /register
参数：username, password, displayName
说明：用户注册。

POST /login
参数：username, password
说明：用户登录，返回用户 ID、用户名及显示名称。

GET /users
参数：无
说明：获取所有注册用户列表，用于客户端展示可分享对象。

POST /upload_image
参数：image (文件流), senderId, recipientId
说明：上传图片并指定接收者，服务器将图片存入 uploads 目录并记录数据库。

GET /get_pending_images
参数：userId
说明：获取指定用户的待接收图片列表（is_received = 0）。

POST /mark_image_received
参数：imageId
说明：标记图片为已接收，并删除服务器本地的图片文件。

GET /uploads/<filename>
参数：文件名
说明：访问已上传的静态图片资源。

## 运行截图
![img.png](img.png)
(注册与登录界面)
![img_1.png](img_1.png)
(分享图片)
![img_2.png](img_2.png)
(传输对象)
![img_3.png](img_3.png)
(选择相册图片)
![img_4.png](img_4.png)
(接收图片)

## 开源协议
本项目采用 MIT License 开源协议。

## 作者
2300310521 马鸿远
GitHub：@Kiri-Colina
如果这个项目对你有帮助，欢迎 Star ⭐！