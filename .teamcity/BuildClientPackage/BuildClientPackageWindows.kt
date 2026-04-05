package buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType

object BuildClientPackageWindows : BuildType({
    configureClientPackageBuildType(
        buildId = "BuildClientPackageWindows",
        buildName = "Build Client Package - Windows",
        scriptRelativePath = ".BD-DevOps/BuildTools/BuildClientPackage/build_windows.py",
        // 如果你们的实际环境要求严格在 Windows Agent 上构建，
        // 可以把这里改成 "Windows"。
        // 当前先不加限制，保持和 Python 脚本能力一致。
        agentOsKeyword = null,
    )
})

