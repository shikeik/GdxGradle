package com.goldsprite.gdxgradle

import org.gradle.api.Project

/**
 * 资源清单生成任务 + 模块依赖绑定
 */
class AssetTasks {

    static void register(Project project, GdxTasksExtension ext) {
        def group = ext.taskGroup.get()
        def rootProject = project.rootProject

        // --- 生成资源清单 (assets.txt) ---
        project.tasks.register('generateAssetList') {
            it.group = group
            it.description = '生成 assets.txt 资源清单文件'
            def assetsPath = "${rootProject.rootDir}/${ext.assetsDir.get()}"
            it.inputs.dir(assetsPath)

            it.doLast {
                File assetsFolder = new File(assetsPath)
                File assetsFile = new File(assetsFolder, "assets.txt")

                assetsFile.delete()
                project.fileTree(assetsFolder).collect { assetsFolder.relativePath(it) }.sort().each {
                    if (it != "assets.txt") assetsFile.append(it + "\n")
                }
                println ">>> 资源清单 assets.txt 已更新"
            }
        }

        // --- 模块依赖绑定 ---
        if (ext.hasLwjgl3.get()) {
            def lwjgl3 = rootProject.findProject(':lwjgl3')
            if (lwjgl3 != null) {
                lwjgl3.afterEvaluate { p ->
                    if (p.tasks.findByName('run'))
                        p.tasks.named('run').configure { dependsOn rootProject.tasks.named('generateAssetList') }
                    if (p.tasks.findByName('processResources'))
                        p.tasks.named('processResources').configure { dependsOn rootProject.tasks.named('generateAssetList') }
                }
            }
        }

        if (ext.hasAndroid.get()) {
            def android = rootProject.findProject(':android')
            if (android != null) {
                android.afterEvaluate { p ->
                    if (p.tasks.findByName('preBuild'))
                        p.tasks.named('preBuild').configure { dependsOn rootProject.tasks.named('generateAssetList') }
                }
            }
        }

        if (ext.hasExamples.get()) {
            def examples = rootProject.findProject(':examples')
            if (examples != null) {
                examples.afterEvaluate { p ->
                    if (p.tasks.findByName('processResources'))
                        p.tasks.named('processResources').configure { dependsOn rootProject.tasks.named('generateAssetList') }
                }
            }
        }
    }
}
