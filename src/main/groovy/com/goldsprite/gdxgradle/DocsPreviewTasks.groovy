package com.goldsprite.gdxgradle

import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec

/**
 * 文档预览任务: previewDocs
 * 启动 DocServer 本地预览
 */
class DocsPreviewTasks {

    static void register(Project project, GdxTasksExtension ext) {
        def group = ext.taskGroup.get()
        def rootProject = project.rootProject
        def mainClass = ext.docsPreviewMainClass.get()

        project.tasks.register('previewDocs', JavaExec) {
            it.group = group
            it.description = '启动本地文档预览服务器'

            // 复用 :core 模块的运行时类路径
            def coreProject = rootProject.findProject(':core')
            if (coreProject != null) {
                it.classpath = coreProject.sourceSets.main.runtimeClasspath
            }

            it.mainClass = mainClass
            it.args = ["${rootProject.rootDir}/docs/engine_docs"]
            it.workingDir = rootProject.rootDir
            it.standardInput = System.in
        }
    }
}
