# NoteLink 链记

> 一款支持**双向链接**的 Android 笔记应用，让想法与想法之间建立联系。

![Platform](https://img.shields.io/badge/platform-Android-green)
![Min SDK](https://img.shields.io/badge/minSdk-26-blue)
![Target SDK](https://img.shields.io/badge/targetSdk-34-blue)
![Version](https://img.shields.io/badge/version-1.0-orange)

---

## ✨ 功能特性

### 📝 核心笔记
- **双向链接**：使用 `[[笔记标题]]` 语法创建笔记内链，点击即可跳转
- **分类管理**：支持自定义分类，长按笔记可快速修改分类
- **置顶笔记**：重要笔记一键置顶，带有 📌 标记
- **搜索**：支持按标题/内容全局搜索

### 📅 日历视图
- 自定义月历组件 `MonthCalendarView`，直观查看每日笔记
- 年月选择器快速跳转

### 🎨 界面与体验
- Material Design 3 风格
- 抽屉侧边栏快速切换笔记与分类
- 夜间模式适配（`values-night`）
- 页面切换动画（slide in/out）
- 插入图片附件

### ⚙️ 设置
- 主题切换（浅色/深色）
- 地区选择（RegionData）
- 天气接口集成（WeatherApi，可扩展）

---

## 🛠️ 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 17 |
| UI | Material Design 3、RecyclerView、DrawerLayout |
| 数据库 | Room（SQLite ORM） |
| 网络 | OkHttp、Gson |
| 架构 | 单 Activity 多 Fragment 风格，Activity 导航 |
| 构建 | Gradle Kotlin DSL、AGP 8.7.3 |

### 主要依赖

```
AndroidX AppCompat 1.6.1
Material 1.10.0
Room 2.6.1
OkHttp 4.12.0
Gson 2.10.1
SwipeRefreshLayout 1.1.0
```

---

## 📂 项目结构

```
app/src/main/java/com/example/myapplication/
├── MainActivity.java          # 主页（笔记列表）
├── EditActivity.java          # 编辑笔记
├── DetailActivity.java        # 笔记详情
├── CalendarActivity.java      # 日历视图
├── SettingsActivity.java      # 设置页
├── NoteAdapter.java           # 笔记列表适配器
├── SidebarAdapter.java        # 侧边栏适配器
├── MonthCalendarView.java     # 自定义月历视图
├── YearMonthPickerDialog.java # 年月选择器
├── data/
│   ├── dao/                  # Room DAO
│   ├── database/             # Room Database
│   └── entity/               # Note、Link 实体
└── util/
    ├── LinkParser.java       # 内链语法解析
    ├── ThemeHelper.java      # 主题工具
    ├── WeatherApi.java       # 天气接口
    └── RegionData.java       # 地区数据
```

---

## 🚀 安装运行

### 要求
- Android Studio Hedgehog | 2023.1.1+
- JDK 17+
- Android SDK Platform 34
- 测试设备 / 模拟器 API 26+

### 步骤

```bash
# 1. 克隆仓库
git clone https://github.com/heluhuo/noteLink.git
cd noteLink

# 2. 用 Android Studio 打开项目，等待 Gradle 同步

# 3. 连接设备或启动模拟器

# 4. 点击 Run ▶️ 或执行：
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 📖 使用说明

### 创建笔记
1. 打开应用 → 主页点击 **+** 新建笔记
2. 填写标题和内容，支持 `[[其他笔记标题]]` 创建内链
3. 点击 **保存**

### 笔记内链
- 在编辑内容中输入 `[[笔记标题]]`，保存后详情页可点击跳转
- 支持跨笔记双向导航

### 置顶笔记
- 长按笔记 → 选择 **📌 置顶**
- 置顶笔记在列表顶部显示，带 📌 标记

### 日历视图
- 底部导航切换到 **日历**
- 点击日期查看当日笔记
- 使用年月选择器快速跳转

---

## 🐛 已知问题

- [ ] 详情页"主页"按钮触摸区域偏小（建议 `minWidth=48dp`）
- [ ] 插入图片后需增强持久化逻辑
- [ ] 天气功能需配置 API Key 后方可使用

---

## 🔮 后续计划

- [ ] 笔记导出（Markdown / PDF）
- [ ] 全文搜索优化（FTS）
- [ ] Widget 桌面小组件
- [ ] 多设备同步（云端备份）
- [ ] 笔记模板功能

---

## 👨‍💻 作者

**heluhuo**

- GitHub：[@heluhuo](https://github.com/heluhuo)

---

## 📄 许可证

本项目目前为个人学习项目，许可证待定。如需使用代码，请联系作者。

---

*⭐ 如果这个项目对你有帮助，欢迎 Star！*
