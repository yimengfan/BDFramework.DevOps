package buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType

object BuildClientPackageIos : BuildType({
    configureClientPackageBuildType(
        buildId = "BuildClientPackageIos",
        buildName = "Build Client Package - iOS",
        scriptRelativePath = ".BD-DevOps/BuildTools/BuildClientPackage/build_ios.py",
        // iOS 只能在 macOS Agent 上构建。
        agentOsKeyword = "Mac",
    )
})

