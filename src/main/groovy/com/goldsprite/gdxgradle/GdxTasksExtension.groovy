package com.goldsprite.gdxgradle

import org.gradle.api.provider.Property
import org.gradle.api.provider.ListProperty

/**
 * GdxTasks 插件扩展配置。
 * 消费端通过 gdxTasks { ... } 块自定义行为。
 */
abstract class GdxTasksExtension {

    // ============================================================
    // 通用配置
    // ============================================================

    /** 任务分组名 (显示在 ./gradlew tasks 中的 group) */
    abstract Property<String> getTaskGroup()

    /** assets 目录 (相对于 rootProject) */
    abstract Property<String> getAssetsDir()

    // ============================================================
    // 模块绑定 (指定项目中存在哪些模块, 用于自动挂载依赖)
    // ============================================================

    /** 是否存在 :lwjgl3 模块 */
    abstract Property<Boolean> getHasLwjgl3()

    /** 是否存在 :android 模块 */
    abstract Property<Boolean> getHasAndroid()

    /** 是否存在 :examples 模块 */
    abstract Property<Boolean> getHasExamples()

    // ============================================================
    // Changelog
    // ============================================================

    /** 回溯最多多少个 Tag 版本 */
    abstract Property<Integer> getMaxVersions()

    /** 每个版本最多抓多少条 Commit */
    abstract Property<Integer> getMaxCommitsPerVersion()

    // ============================================================
    // Docs Preview
    // ============================================================

    /** 文档预览服务器的主类全限定名 (为空则不注册 previewDocs 任务) */
    abstract Property<String> getDocsPreviewMainClass()

    // ============================================================
    // Publish Template
    // ============================================================

    /** 模板根目录 (相对于 rootProject, 为空则不注册 publishTemplate 任务) */
    abstract Property<String> getTemplatesRoot()

    // ============================================================
    // Git Hook
    // ============================================================

    /** 是否自动安装 commit-msg 钩子 */
    abstract Property<Boolean> getInstallGitHook()

    /** commit-msg 允许的类型列表 */
    abstract ListProperty<String> getCommitTypes()
}
