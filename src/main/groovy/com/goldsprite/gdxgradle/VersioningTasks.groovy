package com.goldsprite.gdxgradle

import org.gradle.api.Project

/**
 * 版本管理任务: bumpPatch, bumpMinor, bumpMajor, syncVersions, printVersion
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

        // ═══════════════════════════════════════════════════════════════
        // Task 1: bumpXx - 仅修改 gradle.properties 的版本号
        // ═══════════════════════════════════════════════════════════════

        def bumpVersion = { String type ->
            def props = new Properties()
            versionPropsFile.withInputStream { props.load(it) }

            def currentVersion = props.getProperty('projectVersion')
            def parts = currentVersion.tokenize('.').collect { it.toInteger() }
            def major = parts[0], minor = parts[1], patch = parts[2]

            if (type == 'major') {
                major++; minor = 0; patch = 0
            } else if (type == 'minor') {
                minor++; patch = 0
            } else if (type == 'patch') {
                patch++
            }

            def newVersion = "$major.$minor.$patch"

            // 仅更新 gradle.properties
            def propsContent = versionPropsFile.getText('UTF-8')
            propsContent = propsContent.replaceAll(/projectVersion\s*=\s*.*/, "projectVersion=${newVersion}")
            versionPropsFile.write(propsContent, 'UTF-8')

            println "📝 gradle.properties 已更新: $currentVersion -> $newVersion"
            return newVersion
        }

        // ═══════════════════════════════════════════════════════════════
        // Task 2: syncVersions - 从 gradle.properties 同步到所有文件
        // ═══════════════════════════════════════════════════════════════

        def syncAllVersions = {
            def projectVersion = getCurrentVersion()
            def projectPackage = getProjectProperty('projectPackage')

            println "🔄 开始同步版本号: $projectVersion"

            // 1. 同步 BuildConfig.java
            if (projectPackage != null) {
                def packagePath = projectPackage.replace('.', '/')
                def buildConfigFile = rootProject.file("core/src/main/java/${packagePath}/BuildConfig.java")

                if (buildConfigFile.parentFile.exists() || buildConfigFile.parentFile.mkdirs()) {
                    def jdkVersion = getProjectProperty('jdkVersion') ?: "17"
                    buildConfigFile.text = """package ${projectPackage};
public class BuildConfig {
	public static final String PROJECT_NAME = "${rootProject.name}";
	public static final String DEV_VERSION = "${projectVersion}";
	public static final String JDK_VERSION = "${jdkVersion}";
}"""
                    println "  ✅ BuildConfig.java 已同步"
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

        // bumpXx 任务：只改 gradle.properties，然后调用 syncVersions
        rootProject.tasks.register('bumpPatch') {
            it.group = 'versioning'
            it.description = '升级修订号 (Bug修复): 0.8.11 -> 0.8.12'
            it.doLast {
                bumpVersion('patch')
                // 由于 gradle 配置阶段已结束，需要手动调用同步
                // 使用 finalizedBy 或 dependsOn 确保 syncVersions 执行
            }
            it.finalizedBy 'syncVersions'
        }

        rootProject.tasks.register('bumpMinor') {
            it.group = 'versioning'
            it.description = '升级次版本号 (新功能): 0.8.11 -> 0.9.0'
            it.doLast { bumpVersion('minor') }
            it.finalizedBy 'syncVersions'
        }

        rootProject.tasks.register('bumpMajor') {
            it.group = 'versioning'
            it.description = '升级主版本号 (重大更新): 0.8.11 -> 1.0.0'
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

		syncAllVersions()
    }
}
