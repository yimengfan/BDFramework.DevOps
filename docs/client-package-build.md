# 母包构建链路说明

## 目标

统一母包版本（`clientVersion`）在以下链路中的传递：

1. Unity 编辑器 GUI
2. `BuildTools_ClientPackage.Build(...)`
3. BatchMode / CI 调用
4. 运行时母包版本读取
5. DevOps 产物目录与文档

当前默认版本：`0.1.0`

---

## 相关目录

- GUI 与构建实现：`Packages/com.popo.bdframework/Editor/EditorPipeline/BuildPipeline/BuildPackage/`
- 母包生命周期：`Packages/com.popo.bdframework/Editor/EditorPipeline/Behavior/EditorBehavior/`
- CI 接口：`Packages/com.popo.bdframework/Editor/EditorPipeline/DevOpsPipeline/CI/PublishPipeLineCI.cs`
- Python 入口：`DevOps/CI/BuildClientPackage/`
- 构建输出：`DevOps/PublishPackages/`
- 资源输出：`DevOps/PublishAssets/`

---

## 当前可配置参数

### GUI / CI 公共参数

| 参数 | 含义 | 默认值 | 是否必填 |
| --- | --- | --- | --- |
| `clientVersion` | 母包版本 / 客户端版本号 | `0.1.0` | 建议传入 |

### 自定义构建额外参数（GUI）

| 参数 | 含义 |
| --- | --- |
| `BuildScene` | 打包场景 |
| `BuildSceneConfig` | 场景配置文件 |
| `IsReBuildAssets` | 是否重建资源 |
| `BuildPackageOption` | 资源构建选项 |

---

## GUI 入口

已在以下面板中增加“母包版本”输入框：

- `BuildAndroid`
- `BuildIOS`
- `BuildWindowsPlayer`

该值持久化到：

- `BDFrameworkEditorSetting.BuildClientPackage.ClientVersion`

默认值：`0.1.0`

### 说明

标准构建和自定义构建都会把这个版本号继续传给：

- `BuildTools_ClientPackage.Build(...)`

---

## 核心调用链路

### 1. GUI / CI 进入构建

入口函数：

- `BuildAndroid.Btn_*`
- `BuildIOS.Btn_*`
- `BuildWindowsPlayer.Btn_*`
- `PublishPipeLineCI.BuildClientPackageAndroid()`
- `PublishPipeLineCI.BuildClientPackageIOS()`
- `PublishPipeLineCI.BuildClientPackageWindows()`

### 2. 统一进入母包构建器

核心函数：

- `BuildTools_ClientPackage.Build(BuildMode, bool, string, BuildTarget, BuildPackageOption, string clientVersion)`
- `BuildTools_ClientPackage.Build(BuildMode, string, string, bool, string, BuildTarget, BuildPackageOption, string clientVersion)`

职责：

1. 规范化 `clientVersion`
2. 按构建模式选择 `Debug.bytes` / `Release.bytes`
3. 触发母包生命周期
4. 根据需要执行资源构建
5. 将 `DevOps/PublishAssets/<platform>` 拷贝到 `StreamingAssets`
6. 构建 Android / iOS / Windows 包体
7. 清理临时拷贝内容

### 3. 母包生命周期设置版本号

关键函数：

- `BDFrameworkPipelineHelper.OnBeginBuildPackage(...)`
- `ABDFrameworkPublishPipelineBehaviour.OnBeginBuildPackage(...)`

职责：

- 写入母包脚本版本信息
- 根据 `clientVersion` 设置 `PlayerSettings.bundleVersion`
- Android 递增 `bundleVersionCode`
- iOS 递增 `buildNumber`
- Windows / macOS 使用 `clientVersion`

### 4. 运行时读取版本

关键函数：

- `BDLauncherHotfix.Launch()`

运行时读取：

- `GameBaseConfigProcessor.Config.ClientVersionNum`

该版本用于定位母包/热更资源目录，例如：

- `persistentDataPath/<clientVersion>/<platform>`

---

## 关键函数清单

### 构建入口

- `BuildAndroid.CustomBuild(...)`
- `BuildIOS.CustomBuild(...)`
- `BuildWindowsPlayer.CustomBuild(...)`
- `PublishPipeLineCI.BuildPackage(...)`

### 核心流程

- `BuildTools_ClientPackage.LoadConfig(...)`
- `BuildTools_ClientPackage.CopyDevopsPublishAssetsTo(...)`
- `BuildTools_ClientPackage.BuildAPK(...)`
- `BuildTools_ClientPackage.BuildIpa(...)`
- `BuildTools_ClientPackage.BuildExe(...)`

### 资源构建

- `BuildTools_Assets.BuildAll(...)`

### 生命周期

- `BDFrameworkPipelineHelper.OnBeginBuildPackage(...)`
- `BDFrameworkPipelineHelper.OnEndBuildPackage(...)`
- `ABDFrameworkPublishPipelineBehaviour.OnBeginBuildPackage(...)`

---

## BatchMode / CI 入口

C# CI 方法：

- `BDFramework.Editor.DevOps.PublishPipeLineCI.BuildClientPackageAndroid`
- `BDFramework.Editor.DevOps.PublishPipeLineCI.BuildClientPackageIOS`
- `BDFramework.Editor.DevOps.PublishPipeLineCI.BuildClientPackageWindows`

命令行参数：

- `-clientVersion 0.1.0`

CI 侧通过：

- `Environment.GetCommandLineArgs()`

读取 `-clientVersion`

---

## Python 脚本目录

目录：`DevOps/CI/BuildClientPackage/`

包含：

- `build_android.py`：Android 母包入口（执行主体）
- `build_ios.py`：iOS 母包入口（执行主体）
- `build_windows.py`：Windows 母包入口（执行主体）
- `unity3d_batchmode.py`：仅负责 Unity BatchMode 共通能力
- `config/settings.py`：公共配置
- `common.py`：废弃兼容占位，不再承载流程

当前脚本只要求传一个必要参数：

- `clientVersion`

可选参数：

- `--unity-version`：显式指定 Unity 版本
- `--project-dir`：显式指定 Unity 工程目录
- `--dry-run`：只输出命令，不执行 Unity

### Unity 多版本怎么指定

Python CI 脚本通过以下优先级决定使用哪个 Unity：

1. 环境变量 `UNITY_PATH`
2. 命令行参数 `--unity-version`
3. `config/settings.py` 中的默认版本 `unity.default_version`

`config/settings.py` 中需要维护：

- `unity.default_version`
- `unity.supported_versions`
- `unity.paths`

`unity.paths` 采用 key-value 形式维护 Unity 路径：

1. 第一层 key：宿主机平台 `mac / windows / linux`
2. 第二层 key：`versioned / fallback`
3. 第三层 key：路径名称，如 `hub / ci / legacy`

其中：

- `versioned` 路径需要包含 `{version}` 占位符
- `fallback` 路径用于不区分版本的兜底查找

示例：

```bash
python3 DevOps/CI/BuildClientPackage/build_android.py --client-version 0.1.0 --unity-version 2022.3.74f1
```

### 工程目录怎么传入

通过命令行参数：

- `--project-dir /path/to/unity/project`

如果不传，则按以下顺序兜底：

1. `config/settings.py -> SETTINGS["project"]["default_dir"]`
2. 从脚本目录自动推导当前 Unity 工程根目录

脚本会校验该目录是否具备 Unity 工程必要结构：

- `Assets`
- `Packages`
- `ProjectSettings`

### CI 宿主环境

- Android: `macOS` / `Windows` / `Linux`
- iOS: 仅 `macOS`
- Windows: `macOS` / `Windows` / `Linux`

示例：

```bash
python3 DevOps/CI/BuildClientPackage/build_android.py --client-version 0.1.0
python3 DevOps/CI/BuildClientPackage/build_ios.py --client-version 0.1.0
python3 DevOps/CI/BuildClientPackage/build_windows.py --client-version 0.1.0
python3 DevOps/CI/BuildClientPackage/build_android.py --client-version 0.1.0 --unity-version 2021.3.58f1
python3 DevOps/CI/BuildClientPackage/build_ios.py --client-version 0.1.0 --project-dir /path/to/UnityProject
python3 DevOps/CI/BuildClientPackage/build_android.py --client-version 0.1.0 --dry-run
```

### CI 脚本编写规范

1. `build_xxx.py` 必须是流程主体，不能把完整业务流程重新收口到一个 generic common 模块
2. 共通模块只抽象 Unity BatchMode 能力，例如：
   - 识别宿主系统
   - 解析 Unity 路径
   - 组装 Unity 命令
   - 执行 BatchMode
3. 脚本内部必须加详细注释和流程日志
4. 脚本需要显式处理边界情况：
   - `clientVersion` 为空
   - 指定了不存在的 Unity 版本
   - 指定了非法工程目录
   - 宿主系统不支持目标平台
   - `UNITY_PATH` 不存在
   - 候选 Unity 路径不存在
   - Unity 返回非零退出码

---

## 典型构建流程

### Android / iOS / Windows 共通

1. 确认 `clientVersion`
2. 选择对应配置文件（Debug / Release）
3. 打开场景并加载 `BDLauncher.ConfigText`
4. 临时覆盖构建配置中的 `ClientVersionNum`
5. 构建热更 DLL / SQLite / AssetBundle / `Assets.info`
6. 触发构建包体生命周期
7. 拷贝资源到 `StreamingAssets`
8. 调用平台构建：
   - Android: `BuildAPK(...)`
   - iOS: `BuildIpa(...)`
   - Windows: `BuildExe(...)`
9. 清理 `StreamingAssets`
10. 恢复临时覆盖的配置内容

---

## 产物位置

### 资源产物

- `DevOps/PublishAssets/<platform>/`

### 母包产物

- `DevOps/PublishPackages/<platform>/`

### iOS 导出工程

- `DevOps/PublishPackages/iOS/<application_identifier>/`

---

## 验证建议

### 1. GUI 验证

- 打开 Build 面板
- 修改“母包版本”为一个新值，例如 `0.2.0`
- 执行 Android / iOS / Windows 任意一个标准构建
- 确认日志里有对应版本号

### 2. CI 验证

示例：

```bash
"/Applications/Unity/Hub/Editor/2021.3.58f1/Unity.app/Contents/MacOS/Unity" \
  -batchmode \
  -projectPath "/Users/naipaopao/Documents/GitHub/BDFramework.Core" \
  -executeMethod BDFramework.Editor.DevOps.PublishPipeLineCI.BuildClientPackageAndroid \
  -clientVersion 0.1.0 \
  -quit
```

### 3. 运行时验证

运行母包后，检查：

- `BDLauncherHotfix.Launch()` 日志中的“母包版本”
- 资源目录是否按 `clientVersion` 分层

---

## 后续建议

1. 后续如需扩展 `buildMode`、输出目录、自定义场景等参数，优先加到 Python `config/settings.py`
2. 如果 CI 需要区分 Debug / Release，可在每个平台脚本内固定默认模式，再按需扩展
3. 如需 Jenkins / TeamCity 集成，建议直接调用这些 Python 入口，而不是在流水线中拼接 Unity 命令

