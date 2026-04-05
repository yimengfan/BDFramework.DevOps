package vcsRoots

import jetbrains.buildServer.configs.kotlin.v2019_2.GitVcsRoot

object BDFrameworkCoreRepo : GitVcsRoot({
    id("BDFrameworkCoreRepo")
    name = "BDFramework.Core Repository"

    // 这里直接使用当前仓库的 origin 地址。
    // 如果后续仓库迁移，只需要改这里即可。
    url = "https://github.com/yimengfan/BDFramework.Core.git"

    // TeamCity 项目启用 Versioned Settings 后，通常会以这个仓库作为 settings 来源。
    // 这里显式声明 branchSpec，便于把 VCS 配置也纳入 Kotlin DSL 管理。
    branch = "refs/heads/master"
    branchSpec = """
        +:refs/heads/*
    """.trimIndent()
})

