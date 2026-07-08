plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
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

tasks.register("generateModuleGraph") {
    group = "documentation"
    description = "Generates module dependency graph as DOT file to docs/dependency-graph/modules.dot"
    notCompatibleWithConfigurationCache("Reads live project state at execution time")

    doLast {
        fun Project.nodeId() = path.removePrefix(":").replace(":", "-")

        fun Project.nodeLabel() = path.removePrefix(":").replace(":", ":")

        fun Project.fillColor() =
            when {
                path == ":app" -> "#AED6F1"
                path.startsWith(":feature:") -> "#A9DFBF"
                path == ":core:domain" -> "#D2B4DE"
                path.startsWith(":core:") -> "#FAD7A0"
                else -> "#FFFFFF"
            }

        val projects =
            subprojects
                .filter { it.buildFile.exists() }
                .sortedBy { it.path }
        val projectPaths = projects.map { it.path }.toSet()
        val edges = sortedSetOf(compareBy<Pair<String, String>>({ it.first }, { it.second }))

        projects.forEach { proj ->
            proj.configurations
                .flatMap { it.dependencies }
                .filterIsInstance<ProjectDependency>()
                .filter { dep -> dep.dependencyProject.path in projectPaths }
                .forEach { dep ->
                    val edge = proj.nodeId() to dep.dependencyProject.nodeId()
                    if (edge.first != edge.second) edges.add(edge)
                }
        }

        val dot =
            buildString {
                appendLine("digraph \"modules\" {")
                appendLine("edge [\"dir\"=\"forward\"]")
                appendLine("graph [\"dpi\"=\"150\" \"rankdir\"=\"TB\"]")
                appendLine("node [\"shape\"=\"rectangle\" \"style\"=\"filled\"]")
                appendLine()
                projects.forEach { p ->
                    appendLine("\"${p.nodeId()}\" [\"label\"=\"${p.nodeLabel()}\" \"fillcolor\"=\"${p.fillColor()}\"]")
                }
                appendLine()
                appendLine("// edges")
                edges.forEach { (from, to) -> appendLine("\"$from\" -> \"$to\"") }
                append("}")
            }

        file("docs/dependency-graph/modules.dot").apply {
            parentFile.mkdirs()
            writeText(dot)
        }
        logger.lifecycle("Generated: docs/dependency-graph/modules.dot")
    }
}
