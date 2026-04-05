import buildTypes.BuildClientPackageAndroid
import buildTypes.BuildClientPackageIos
import buildTypes.BuildClientPackageWindows
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import vcsRoots.BDFrameworkCoreRepo

object BDFrameworkCoreProject : Project({
    id("BDFrameworkCore")
    name = "BDFramework.Core"
    description = "TeamCity Kotlin DSL skeleton for BDFramework.Core client package builds."

    params {
        // Python 可执行程序。
        // macOS / Linux 推荐 python3，Windows 可改为 python 或 py。
        param("python.executable", "python3")

        // 母包构建参数。
        param("client.version", "0.1.0")
        param("unity.version", "2021.3.58f1")
        param("project.dir", ".")

        // dry-run 参数通过字符串透传，避免在 DSL 里写过多分支逻辑。
        // 为空表示真正执行；设为 --dry-run 表示只打印命令不真正调用 Unity。
        param("build.dryRun.arg", "")
    }

    vcsRoot(BDFrameworkCoreRepo)

    buildType(BuildClientPackageAndroid)
    buildType(BuildClientPackageIos)
    buildType(BuildClientPackageWindows)
})


