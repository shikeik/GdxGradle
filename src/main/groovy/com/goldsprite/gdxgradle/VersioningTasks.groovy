package com.goldsprite.gdxgradle

import org.gradle.api.Project
import groovy.text.GStringTemplateEngine

/**
 * 版本管理任务: bumpPatch, bumpMinor, bumpMajor, bumpBuild, syncVersions, printVersion
 *
 * 架构说明:
 * - bumpXx: 仅修改 gradle.properties 中的 projectVersion
 * - syncVersions: 从 gradle.properties 同步版本号到所有相关文件
 * - 统一流向: gradle.properties -> BuildConfig.java + README.md + android/build.gradle
 */
class VersioningTasks {

	static void register(Project project, GdxTasksExtension ext) {
		def rootProject = project.rootProject

		// 版本任务操作根 gradle.properties，只需在根项目注册一次
		// 多子模块场景下，插件可能被多个子项目应用，跳过重复注册避免 bump N 次
		if (rootProject.tasks.findByName('bumpPatch') != null) return

		def versionPropsFile = rootProject.file('gradle.properties')
		def androidBuildFile = rootProject.file('android/build.gradle')
		def readmeFile = rootProject.file('README.md')

		// ═══════════════════════════════════════════════════════════════
		// 工具方法
		// ═══════════════════════════════════════════════════════════════

		// 读取当前版本
		def getCurrentVersion = {
			def props = new Properties()
			versionPropsFile.withInputStream { props.load(it) }
			return props.getProperty('projectVersion')
		}

		// 从 gradle.properties 读取属性
		def getProjectProperty = { String key ->
			def props = new Properties()
			versionPropsFile.withInputStream { props.load(it) }
			return props.getProperty(key)
		}

		// 解析版本号，返回 [parts, hasBuildSuffix]
		// 0.10.9 -> [[0, 10, 9], false]
		// 0.10.9.0_build -> [[0, 10, 9, 0], true]
		def parseVersion = { String version ->
			def hasBuildSuffix = version.endsWith('_build')
			def cleanVersion = hasBuildSuffix ? version.substring(0, version.length() - 6) : version
			def parts = cleanVersion.tokenize('.').collect { it.toInteger() }
			return [parts, hasBuildSuffix]
		}

		// 更新 gradle.properties 中的版本号
		def updateVersion = { String newVersion ->
			def propsContent = versionPropsFile.getText('UTF-8')
			propsContent = propsContent.replaceAll(/projectVersion\s*=\s*.*/, "projectVersion=${newVersion}")
			versionPropsFile.write(propsContent, 'UTF-8')
		}

		// 从版本号提取构建号
		def getBuildNumber = { String version ->
			def (parts, hasBuildSuffix) = parseVersion(version)
			if (hasBuildSuffix && parts.size() >= 4) {
				return parts[-1]
			}
			return 0
		}

		// ═══════════════════════════════════════════════════════════════
		// 构建流水号管理
		// ═══════════════════════════════════════════════════════════════

		def buildCountFile = rootProject.file('build.properties')

		// 读取当前构建流水号
		def getBuildCount = {
			if (!buildCountFile.exists()) return 0
			def props = new Properties()
			buildCountFile.withInputStream { props.load(it) }
			return props.getProperty('buildCount', '0').toInteger()
		}

		// 读取上次同步的版本号
		def getLastSyncedVersion = {
			if (!buildCountFile.exists()) return ''
			def props = new Properties()
			buildCountFile.withInputStream { props.load(it) }
			return props.getProperty('lastVersion', '')
		}

		// 更新上次同步的版本号
		def updateLastSyncedVersion = { String version ->
			def props = new Properties()
			if (buildCountFile.exists()) {
				buildCountFile.withInputStream { props.load(it) }
			}
			props.setProperty('lastVersion', version)
			buildCountFile.withOutputStream { 
				props.store(it, "项目总构建流水号\n每次构建产物或版本变更时自动递增，用于追踪项目生命周期内的总构建次数") 
			}
		}

		// 递增构建流水号（带防重复标记）
		def incrementBuildCount = {
			// 防止一次 Gradle 构建周期内多次递增
			if (rootProject.ext.has('buildCountIncremented')) {
				return rootProject.ext.buildCount
			}

			def count = getBuildCount() + 1
			def props = new Properties()
			if (buildCountFile.exists()) {
				buildCountFile.withInputStream { props.load(it) }
			}
			props.setProperty('buildCount', count.toString())
			buildCountFile.withOutputStream { 
				props.store(it, "项目总构建流水号\n每次构建产物或版本变更时自动递增，用于追踪项目生命周期内的总构建次数") 
			}

			rootProject.ext.buildCountIncremented = true
			rootProject.ext.buildCount = count

			println "🔢 构建流水号递增: #${count}"
			return count
		}

		// ═══════════════════════════════════════════════════════════════
		// Task 1: bumpXx - 仅修改 gradle.properties 的版本号
		// ═══════════════════════════════════════════════════════════════

		// bumpBuild: 添加或递增 build 版本号
		def bumpBuildVersion = {
			def currentVersion = getCurrentVersion()
			def (parts, hasBuildSuffix) = parseVersion(currentVersion)
			def newVersion

			if (hasBuildSuffix) {
				// 已有 _build 后缀，递增最后一个数字
				def buildNum = parts[-1]
				parts[-1] = buildNum + 1
				newVersion = parts.join('.') + '_build'
			} else {
				// 无后缀，添加 .0_build
				newVersion = currentVersion + '.0_build'
			}

			updateVersion(newVersion)
			println "📝 gradle.properties 已更新: $currentVersion -> $newVersion"
			return newVersion
		}

		// bumpMajor/bumpMinor/bumpPatch: 去除 build 后缀后升级
		def bumpVersion = { String type ->
			def currentVersion = getCurrentVersion()
			def (parts, hasBuildSuffix) = parseVersion(currentVersion)

			// 确保至少有3个部分 (major, minor, patch)
			while (parts.size() < 3) {
				parts.add(0)
			}

			def major = parts[0], minor = parts[1], patch = parts[2]

			if (type == 'major') {
				major++; minor = 0; patch = 0
			} else if (type == 'minor') {
				minor++; patch = 0
			} else if (type == 'patch') {
				patch++
			}

			def newVersion = "$major.$minor.$patch"
			updateVersion(newVersion)

			println "📝 gradle.properties 已更新: $currentVersion -> $newVersion"
			return newVersion
		}

		// ═══════════════════════════════════════════════════════════════
		// Task 2: syncVersions - 从 gradle.properties 同步到所有文件
		// ═══════════════════════════════════════════════════════════════

		def syncAllVersions = {
			def projectVersion = getCurrentVersion()
			def projectPackage = getProjectProperty('projectPackage')
			def jdkVersion = getProjectProperty('jdkVersion') ?: "17"
			def buildNumber = getBuildNumber(projectVersion)

			// 检测版本变化，变化时递增构建号
			def lastSyncedVersion = getLastSyncedVersion()
			if (lastSyncedVersion != projectVersion) {
				incrementBuildCount()
				updateLastSyncedVersion(projectVersion)
			}
			def buildCount = getBuildCount()

			println "🔄 开始同步版本号: $projectVersion (构建流水号: #${buildCount})"

			// 1. 同步 BuildConfig.java
			if (projectPackage != null) {
				def packagePath = projectPackage.replace('.', '/')
				def buildConfigFile = rootProject.file("core/src/main/java/${packagePath}/BuildConfig.java")

				if (buildConfigFile.parentFile.exists() || buildConfigFile.parentFile.mkdirs()) {
					def template

					// 检查用户自定义模板
					def customTemplateFile = ext.buildConfig?.templateFile?.getOrNull()
					if (customTemplateFile?.exists()) {
						template = customTemplateFile.text
						println "  📄 使用自定义模板: ${customTemplateFile}"
					} else {
						// 加载内置模板
						def stream = getClass().getResourceAsStream("/templates/BuildConfig.java.groovy")
						if (stream == null) {
							throw new RuntimeException("找不到内置模板: /templates/BuildConfig.java.groovy")
						}
						template = stream.text
					}

					// 构建渲染上下文
					def ctx = [
						rootProject: rootProject,
						projectName: rootProject.name,
						projectVersion: projectVersion,
						projectPackage: projectPackage,
						jdkVersion: jdkVersion,
						buildNumber: buildNumber,
						buildCount: buildCount
					]

					try {
						def engine = new GStringTemplateEngine()
						def result = engine.createTemplate(template).make(ctx)
						buildConfigFile.text = result.toString()
						println "  ✅ BuildConfig.java 已同步"
					} catch (Exception e) {
						println "  ❌ BuildConfig.java 生成失败: ${e.message}"
						throw e
					}
				}
			}

			// 2. 同步 README.md
			if (readmeFile.exists()) {
				def content = readmeFile.getText('UTF-8')
				def updatedContent = content.replaceFirst(/^(# ${rootProject.name}.*V)(.*)/) {
					all, prefix, oldVersion -> "${prefix}${projectVersion}"
				}
				readmeFile.write(updatedContent, 'UTF-8')
				println "  ✅ README.md 已同步"
			}

			// 3. 同步 android/build.gradle (解决 AIDE+ 硬编码问题)
			if (androidBuildFile.exists()) {
				def androidContent = androidBuildFile.getText('UTF-8')

				// 提取当前 versionCode
				def currentVersionCode = 0
				androidContent.eachMatch(/versionCode\s+(\d+)/) { match ->
					currentVersionCode = match[1].toInteger()
				}

				// 更新 versionName
				androidContent = androidContent.replaceAll(/versionName\s+"[^"]*"/, "versionName \"${projectVersion}\"")

				// 递增 versionCode (仅在 versionName 发生变化时)
				def oldVersionName = ""
				androidContent.eachMatch(/versionName\s+"([^"]*)"/) { match ->
					oldVersionName = match[1]
				}

				androidBuildFile.write(androidContent, 'UTF-8')
				println "  ✅ android/build.gradle 已同步 (versionName: ${projectVersion})"
			}

			println "🎉 版本同步完成: ${projectVersion}"
		}

		// ═══════════════════════════════════════════════════════════════
		// 注册任务
		// ═══════════════════════════════════════════════════════════════

		// markBuildRelease 任务：标记构建产物生成，递增流水号
		rootProject.tasks.register('markBuildRelease') {
			it.group = 'build'
			it.description = '标记构建产物生成，递增构建流水号'
			it.doLast { incrementBuildCount() }
		}

		// bumpBuild 任务：添加或递增 build 版本号
		rootProject.tasks.register('bumpBuild') {
			it.group = 'versioning'
			it.description = '添加或递增构建号: 0.10.9 -> 0.10.9.0_build, 0.10.9.0_build -> 0.10.9.1_build'
			it.doLast { bumpBuildVersion() }
			it.finalizedBy 'syncVersions'
		}

		// bumpXx 任务：只改 gradle.properties，然后调用 syncVersions
		rootProject.tasks.register('bumpPatch') {
			it.group = 'versioning'
			it.description = '升级修订号 (Bug修复): 0.8.11 -> 0.8.12 (自动去除 _build 后缀)'
			it.doLast { bumpVersion('patch') }
			it.finalizedBy 'syncVersions'
		}

		rootProject.tasks.register('bumpMinor') {
			it.group = 'versioning'
			it.description = '升级次版本号 (新功能): 0.8.11 -> 0.9.0 (自动去除 _build 后缀)'
			it.doLast { bumpVersion('minor') }
			it.finalizedBy 'syncVersions'
		}

		rootProject.tasks.register('bumpMajor') {
			it.group = 'versioning'
			it.description = '升级主版本号 (重大更新): 0.8.11 -> 1.0.0 (自动去除 _build 后缀)'
			it.doLast { bumpVersion('major') }
			it.finalizedBy 'syncVersions'
		}

		// 同步任务：从 gradle.properties 同步到所有文件
		rootProject.tasks.register('syncVersions') {
			it.group = 'versioning'
			it.description = '从 gradle.properties 同步版本号到所有文件 (BuildConfig.java, README.md, android/build.gradle)'
			it.doLast { syncAllVersions() }
		}

		// 打印版本
		rootProject.tasks.register('printVersion') {
			it.group = 'versioning'
			it.description = '打印当前版本号'
			it.doLast {
				println "Current Version: ${getCurrentVersion()}"
			}
		}

		// 注意：不再自动执行 syncAllVersions()
		// 需要手动运行 ./gradlew syncVersions
	}
}
