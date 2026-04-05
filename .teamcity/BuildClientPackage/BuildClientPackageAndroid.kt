package buildTypes

import jetbrains.buildServer.configs.kotlin.BuildType

object BuildClientPackageAndroid : BuildType({
    configureClientPackageBuildType(
        buildId = "BuildClientPackageAndroid",
        buildName = "Build Client Package - Android",
        scriptRelativePath = ".BD-DevOps/BuildTools/BuildClientPackage/build_android.py",
        // Android 脚本本身支持 mac / windows / linux。
        // 这里不额外加 Agent OS 限制，交给脚本与实际环境决定。
        agentOsKeyword = null,
    )
})



