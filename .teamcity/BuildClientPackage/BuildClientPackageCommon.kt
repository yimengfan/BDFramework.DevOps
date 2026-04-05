package buildTypes

import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import vcsRoots.BDFrameworkCoreRepo

fun BuildType.configureClientPackageBuildType(
    buildId: String,
    buildName: String,
    scriptRelativePath: String,
    agentOsKeyword: String? = null,
){
    id(buildId)
    name = buildName
    description = "Call ${scriptRelativePath} to build client package in batchmode."

    vcs {
        root(BDFrameworkCoreRepo)
        cleanCheckout = true
    }

    steps {
        script {
            name = "Run ${buildName}"
            scriptContent = """
                "%python.executable%" "${scriptRelativePath}" --client-version "%client.version%" --unity-version "%unity.version%" --project-dir "%project.dir%" %build.dryRun.arg%
            """.trimIndent()
        }
    }

    artifactRules = "DevOps/CI/BuildClientPackage/logs => logs"

    requirements {
        if (agentOsKeyword != null) {
            contains("teamcity.agent.jvm.os.name", agentOsKeyword)
        }
    }
}



