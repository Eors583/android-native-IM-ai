pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 所有仓库只在这里声明，其他文件不再重复
        google()
        mavenCentral()
    }
}

rootProject.name = "AIIMChat"
include(":app")