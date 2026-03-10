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

	/**
	 * commit-msg 允许的类型列表 (向后兼容，字符串格式)
	 * 建议使用 commitTypeDefs 获得更灵活的配置
	 */
	abstract ListProperty<String> getCommitTypes()

	/**
	 * 详细的提交类型定义（包含描述）
	 * 覆盖此列表将完全替换默认类型
	 */
	abstract ListProperty<CommitType> getCommitTypeDefs()

	/** 是否使用详细的类型定义（默认为 false，使用 commitTypes 列表） */
	abstract Property<Boolean> getUseDetailedCommitTypes()

	/** 自动跳过的提交模式（正则表达式列表） */
	abstract ListProperty<String> getSkipPatterns()

	/** 是否启用默认的跳过模式（Merge、Revert 等） */
	abstract Property<Boolean> getEnableDefaultSkipPatterns()

	// ============================================================
	// 便捷方法：用于 DSL 配置
	// ============================================================

	/**
	 * 便捷方法：添加单个提交类型定义
	 * 用法：commitType('feat', '新功能')
	 */
	void commitType(String type, String description) {
		def currentList = new ArrayList<>(commitTypeDefs.getOrElse([]))
		currentList.add(new CommitType(type, description))
		commitTypeDefs.set(currentList)
	}

	/**
	 * 便捷方法：批量添加提交类型定义
	 * 用法：commitTypes(['feat:新功能', 'fix:修复'])
	 */
	void commitTypes(List<String> typeDefs) {
		def list = typeDefs.collect { typeDef ->
			def parts = typeDef.split(':', 2)
			new CommitType(parts[0], parts.length > 1 ? parts[1] : parts[0])
		}
		// 合并到现有列表而非替换
		def currentList = new ArrayList<>(commitTypeDefs.getOrElse([]))
		currentList.addAll(list)
		commitTypeDefs.set(currentList)
	}
}
