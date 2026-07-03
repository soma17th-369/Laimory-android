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
        google()
        mavenCentral()
    }
}

rootProject.name = "Laimory"

include(":app")
include(":feature:home")
include(":feature:feature1")
include(":core:domain")
include(":core:data")
include(":core:ui")
include(":core:common")
