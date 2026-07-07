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
include(":feature:collection")
include(":core:domain")
include(":core:data")
include(":core:collection")
include(":core:ui")
include(":core:util")
