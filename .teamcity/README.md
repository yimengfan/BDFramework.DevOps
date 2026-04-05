# TeamCity Kotlin DSL Skeleton

这套 `.teamcity/` 目录用于把 `BDFramework.Core` 的母包构建流程以 **纯代码** 形式托管到 VCS。

当前已经内置 3 个 TeamCity Build Configuration：

- `Build Client Package - Android`
- `Build Client Package - iOS`
- `Build Client Package - Windows`

这些任务不会把具体业务逻辑写进 TeamCity DSL，而是直接调用仓库中已经存在的 Python 脚本：

- `DevOps/CI/BuildClientPackage/build_android.py`
- `DevOps/CI/BuildClientPackage/build_ios.py`
- `DevOps/CI/BuildClientPackage/build_windows.py`

这样做的好处：

1. 本地可复现
2. 不绑定某一个 CI 平台
3. TeamCity 只负责参数、Agent 选择、触发器和产物归档
4. 业务构建逻辑继续由仓库代码维护

---

## 当前参数设计

项目级参数定义在：

- `.teamcity/Project.kt`

默认包含：

- `python.executable`：Python 可执行程序，默认 `python3`
- `client.version`：母包版本，默认 `0.1.0`
- `unity.version`：Unity 版本，默认 `2021.3.58f1`
- `project.dir`：Unity 工程目录，默认 `.`
- `build.dryRun.arg`：为空表示真实执行；设为 `--dry-run` 表示只打印命令

---

## VCS、Trigger、Params 应该怎么写

### 1. VCS Root 建议写法

建议把仓库定义单独放到：

- `.teamcity/vcsRoots/BDFrameworkCoreRepo.kt`

当前骨架已经这样做了。BuildType 中统一引用：

```kotlin
vcs {
    root(BDFrameworkCoreRepo)
    cleanCheckout = true
}
```

这样做的好处：

1. 仓库地址、分支规则走代码评审
2. 多个 BuildType 复用同一个 VCS Root
3. 仓库变更只改一个地方

### 2. Params 建议写法

建议分两层：

#### Project 级参数

放稳定默认值：

- `python.executable`
- `unity.version`
- `project.dir`

#### Build 级参数

放单个任务可能覆盖的值，例如：

- `client.version`
- `build.dryRun.arg`

BuildType 中如果要覆盖默认值，可以写：

```kotlin
params {
    param("build.dryRun.arg", "--dry-run")
}
```

### 3. Trigger 建议写法

建议先手动运行验证通过后，再加 VCS Trigger。

一个常见的 VCS Trigger 示例：

```kotlin
triggers {
    vcs {
        branchFilter = "+:<default>"
        triggerRules = """
            +:.teamcity/**
            +:DevOps/CI/**
            +:DevOps/docs/**
            +:Packages/**
            +:ProjectSettings/**
            -:Library/**
            -:Temp/**
            -:DevOps/PublishAssets/**
            -:DevOps/PublishPackages/**
        """.trimIndent()
    }
}
```

推荐理解方式：

1. 只监听默认分支
2. 只关注 CI、配置、工程代码、项目设置目录
3. 忽略缓存目录和产物目录

如果你们后续要把 Trigger 正式落到 DSL，我建议：

- Android：先加 VCS Trigger
- iOS：加 VCS Trigger，但只跑在 mac Agent
- Windows：看你们是否真的允许非 Windows Agent 构建，再决定触发策略

---

## TeamCity 怎么使用

### 方案一：从零启用 Versioned Settings

1. 把这套 `.teamcity/` 目录提交到仓库
2. 在 TeamCity 中创建一个 Project
3. 给这个 Project 关联当前仓库的 VCS Root
4. 进入：
   - `Project Settings`
   - `Versioned Settings`
5. 选择：
   - `Enable settings synchronization`
6. Format 选择：
   - `Kotlin`
7. Settings path 选择：
   - `.teamcity`
8. 保存后 TeamCity 会读取 `settings.kts`
9. 成功后你会看到 3 个 Build Configuration 自动出现

### 方案二：已有 Project，切换为代码托管

1. 先备份现有 UI 配置
2. 打开该 Project 的 `Versioned Settings`
3. 启用 Kotlin DSL
4. 指向当前仓库与 `.teamcity/`
5. 让 TeamCity 从仓库同步配置

---

## 三个平台任务如何传参

TeamCity 任务会最终执行类似下面的命令：

```bash
"%python.executable%" "DevOps/CI/BuildClientPackage/build_android.py" --client-version "%client.version%" --unity-version "%unity.version%" --project-dir "%project.dir%" %build.dryRun.arg%
```

iOS / Windows 同理，只是脚本不同。

### 常用参数示例

#### 真正执行

- `client.version = 0.1.0`
- `unity.version = 2021.3.58f1`
- `project.dir = .`
- `build.dryRun.arg = `（留空）

#### 只做命令验证

- `build.dryRun.arg = --dry-run`

---

## Agent 要求

当前默认策略：

- Android：不额外限制 Agent OS
- iOS：限制在 macOS Agent
- Windows：暂不额外限制 Agent OS

如果你们后续希望强制 Windows 包只能在 Windows Agent 上构建，可修改：

- `.teamcity/buildTypes/BuildClientPackageWindows.kt`

把：

- `agentOsKeyword = null`

改成：

- `agentOsKeyword = "Windows"`

---

## 建议的 TeamCity 参数管理方式

### Project 级参数

放稳定默认值：

- `python.executable`
- `unity.version`
- `project.dir`

### Build 级参数

放每次可能变化的值：

- `client.version`
- `build.dryRun.arg`

---

## 建议的第一次验证流程

1. 先把 `build.dryRun.arg` 设为 `--dry-run`
2. 手动运行 Android 任务
3. 检查日志里是否正确输出：
   - `client.version`
   - `unity.version`
   - `project.dir`
   - Python 脚本路径
4. 确认命令无误后，再把 `build.dryRun.arg` 清空
5. 再执行真实构建

---

## 维护建议

1. TeamCity DSL 只保留流程编排，不承载具体构建业务
2. Python 脚本继续作为跨 CI 平台共享入口
3. 如果后续要增加：
   - 资源构建
   - 单元测试
   - 发布上传
   建议继续在 `.teamcity/buildTypes/` 下新增独立 BuildType
4. 如果流程开始复用，可以继续加 TeamCity Template，但当前 skeleton 保持最小可读即可


