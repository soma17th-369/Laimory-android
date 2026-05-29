plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.dependency.graph.generator)
}

ktlint {
    version.set("1.3.0")
    android.set(true)
    outputToConsole.set(true)
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

dependencyGraphGenerator {
    projectGenerators.add(
        com.vanniktech.dependency.graph.generator.DependencyGraphGeneratorExtension.ProjectGenerator(
            name = "modules",
        ),
    )
}
