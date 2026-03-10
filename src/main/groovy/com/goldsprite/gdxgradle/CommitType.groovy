package com.goldsprite.gdxgradle

import groovy.transform.CompileStatic
import groovy.transform.ToString

/**
 * 提交类型定义
 * 包含类型标识和对应的描述信息，用于 GitHook 校验和提示
 */
@CompileStatic
@ToString(includeNames = true)
class CommitType {
	/** 类型标识，如 feat、fix 等 */
	String type

	/** 描述说明，用于校验失败时的提示 */
	String description

	/** 是否为特殊类型（如 Merge、Revert），不需要遵循标准格式 */
	boolean special = false

	CommitType(String type, String description) {
		this.type = type
		this.description = description
		this.special = false
	}

	CommitType(String type, String description, boolean special) {
		this.type = type
		this.description = description
		this.special = special
	}

	/**
	 * 预定义的标准提交类型
	 */
	static List<CommitType> defaults() {
		return [
			new CommitType('feat', '新功能'),
			new CommitType('fix', '修复'),
			new CommitType('perf', '性能优化'),
			new CommitType('docs', '文档'),
			new CommitType('refactor', '重构'),
			new CommitType('chore', '杂项/构建'),
			new CommitType('test', '测试'),
			new CommitType('assets', '资源'),
		]
	}

	/**
	 * 预定义的跳过模式（Git 自动生成的特殊提交）
	 * 这些提交不需要遵循标准格式
	 */
	static List<String> defaultSkipPatterns() {
		return [
			'^Merge ',           // Merge branch 'xxx' into 'yyy'
			"^Revert \"",        // Revert "xxx" (双引号包裹的内容)
			"^Revert " + "'",    // Revert 'xxx' (单引号包裹的内容)
			'^fixup!',           // git commit --fixup
			'^squash!',          // git commit --squash
			'^amend!',           // git commit --amend (带标记)
			'^WIP[: ]',          // Work In Progress
			'^Auto-merged ',     // 某些 IDE 自动合并
			'^Branch: ',         // 某些 Git 工作流
		]
	}
}
