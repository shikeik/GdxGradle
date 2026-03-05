package com.goldsprite.gdxgradle

import org.gradle.api.Project

/**
 * 版本管理任务: bumpPatch, bumpMinor, bumpMajor, printVersion
 */
class VersioningTasks {

    static void register(Project project, GdxTasksExtension ext) {
        def rootProject = project.rootProject

        // 版本任务操作根 gradle.properties，只需在根项目注册一次
        // 多子模块场景下，插件可能被多个子项目应用，跳过重复注册避免 bump N 次
        if (rootProject.tasks.findByName('bumpPatch') != null) return

        def versionPropsFile = rootProject.file('gradle.properties')
        def androidBuildFile = rootProject.file('android/build.gradle')

        // 读取当前版本的闭包
        def getCurrentVersion = {
            def props = new Properties()
            versionPropsFile.withInputStream { props.load(it) }
            return props.getProperty('projectVersion')
        }

        // 更新版本号核心逻辑
        def updateVersion = { String type ->
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

            // 1. 更新 gradle.properties
            def propsContent = versionPropsFile.getText('UTF-8')
            propsContent = propsContent.replaceAll(/projectVersion\s*=\s*.*/, "projectVersion=${newVersion}")
            versionPropsFile.write(propsContent, 'UTF-8')

            // 2. 更新 android/build.gradle (解决 AIDE+ 硬编码问题)
            if (androidBuildFile.exists()) {
                def androidContent = androidBuildFile.getText('UTF-8')
                androidContent = androidContent.replaceAll(/versionName\s+"[^"]*"/, "versionName \"${newVersion}\"")
                androidContent = androidContent.replaceAll(/versionCode\s+(\d+)/) { match, code ->
                    "versionCode ${code.toInteger() + 1}"
                }
                androidBuildFile.write(androidContent, 'UTF-8')
            }

            println "🎉 版本号已升级: $currentVersion -> $newVersion"
            println "✅ gradle.properties 已更新"
            if (androidBuildFile.exists()) println "✅ android/build.gradle 已更新 (versionName & versionCode)"
        }

        // 注册到根项目，确保 gradlew bumpPatch 只执行一次
        rootProject.tasks.register('bumpPatch') {
            it.group = 'versioning'
            it.description = '升级修订号 (Bug修复): 0.8.11 -> 0.8.12'
            it.doLast { updateVersion('patch') }
        }

        rootProject.tasks.register('bumpMinor') {
            it.group = 'versioning'
            it.description = '升级次版本号 (新功能): 0.8.11 -> 0.9.0'
            it.doLast { updateVersion('minor') }
        }

        rootProject.tasks.register('bumpMajor') {
            it.group = 'versioning'
            it.description = '升级主版本号 (重大更新): 0.8.11 -> 1.0.0'
            it.doLast { updateVersion('major') }
        }

        rootProject.tasks.register('printVersion') {
            it.group = 'versioning'
            it.description = '打印当前版本号'
            it.doLast {
                println "Current Version: ${getCurrentVersion()}"
            }
        }
    }
}
