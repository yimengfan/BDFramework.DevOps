package buildTypes

import jetbrains.buildServer.configs.kotlin.BuildType

object BuildClientPackageIos : BuildType({
    configureClientPackageBuildType(
        buildId = "BuildClientPackageIos",
        buildName = "Build Client Package - iOS",
        scriptRelativePath = "DevOps/CI/BuildClientPackage/build_ios.py",
        // iOS 只能在 macOS Agent 上构建。
        agentOsKeyword = "Mac",
    )
})

