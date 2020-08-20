package com.github.cchabanois.intellijmesfavoris.services

import com.intellij.openapi.project.Project
import com.github.cchabanois.intellijmesfavoris.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
