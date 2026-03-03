package com.goldsprite.gdxgradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * GdxTasks 主插件入口。
 * 注册扩展 -> 设置默认值 -> afterEvaluate 中注册所有任务。
 */
class GdxTasksPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        // 1. 注册扩展
        def ext = project.extensions.create('gdxTasks', GdxTasksExtension)

        // 2. 设置默认值
        ext.taskGroup.convention('gdx-tasks')
        ext.assetsDir.convention('assets')
        ext.hasLwjgl3.convention(true)
        ext.hasAndroid.convention(true)
        ext.hasExamples.convention(true)
        ext.maxVersions.convention(100)
        ext.maxCommitsPerVersion.convention(500)
        ext.docsPreviewMainClass.convention('')
        ext.templatesRoot.convention('')
        ext.installGitHook.convention(true)
        ext.commitTypes.convention(['feat', 'fix', 'perf', 'docs', 'refactor', 'chore', 'test', 'assets'])

        // 3. afterEvaluate 确保用户配置已生效再注册任务
        project.afterEvaluate {
            def group = ext.taskGroup.get()

            // --- 通用任务 ---
            VersioningTasks.register(project, ext)
            AssetTasks.register(project, ext)
            ChangelogTasks.register(project, ext)
            JPackageTasks.register(project, ext)
            PublishDocsTasks.register(project, ext)

            // --- 条件任务 ---
            if (ext.docsPreviewMainClass.get()) {
                DocsPreviewTasks.register(project, ext)
            }
            if (ext.templatesRoot.get()) {
                PublishTemplateTasks.register(project, ext)
            }

            // --- Git Hook ---
            if (ext.installGitHook.get()) {
                GitHookInstaller.install(project, ext)
            }
        }
    }
}
