package com.goldsprite.gdxgradle

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.internal.jvm.Jvm

/**
 * JPackage 打包任务: jpackageExe, packageSingleExe
 * 注册到 :lwjgl3 子项目中
 */
class JPackageTasks {

    static void register(Project project, GdxTasksExtension ext) {
        def rootProject = project.rootProject
        def lwjgl3 = rootProject.findProject(':lwjgl3')
        if (lwjgl3 == null) return

        lwjgl3.afterEvaluate { lwjgl3Project ->
            registerJpackageExe(lwjgl3Project, ext)
            registerPackageSingleExe(lwjgl3Project, ext)

            // 根项目别名
            rootProject.tasks.register('oneClickRelease') {
                it.group = 'distribution'
                it.description = '一键构建 Jar -> jpackage -> 单文件 Exe'
                it.dependsOn lwjgl3Project.tasks.named('packageSingleExe')
                it.doLast {
                    println "🎉 发布流程结束"
                }
            }
            rootProject.tasks.register('packageSingleExe') {
                it.group = 'distribution'
                it.description = 'Package single exe (Alias for :lwjgl3:packageSingleExe)'
                it.dependsOn lwjgl3Project.tasks.named('packageSingleExe')
            }
        }
    }

    private static void registerJpackageExe(Project lwjgl3Project, GdxTasksExtension ext) {
        def rootProject = lwjgl3Project.rootProject

        lwjgl3Project.tasks.register('jpackageExe') {
            it.group = 'distribution'
            it.description = '使用 jpackage 打包为自带运行时的应用程序镜像 (App Image)'
            it.dependsOn 'jar'

            it.doLast {
                def rootDir = rootProject.projectDir
                def appName = rootProject.name
                def projectVersion = rootProject.findProperty('projectVersion')
                def jarName = "${appName}_V${projectVersion}.jar"
                def inputJar = lwjgl3Project.file("${rootDir}/outputs/${jarName}")
                def stagingDir = lwjgl3Project.file("${lwjgl3Project.buildDir}/jpackage-staging")
                def outputDir = lwjgl3Project.file("${rootDir}/outputs/jpackage")
                def iconPath = "${rootDir}/assets/icon.ico"
                def mainClass = lwjgl3Project.mainClassName

                if (!inputJar.exists()) {
                    throw new GradleException("找不到输入 Jar 文件: ${inputJar}\n请确保 jar 任务已成功运行.")
                }

                lwjgl3Project.delete(stagingDir)
                stagingDir.mkdirs()
                lwjgl3Project.copy { from inputJar; into stagingDir }
                lwjgl3Project.delete(outputDir)

                println "📦 开始使用 jpackage 打包..."
                println "   - 输入 Jar: ${jarName}"
                println "   - 输出目录: ${outputDir}"
                println "   - 主类: ${mainClass}"

                def withConsole = rootProject.hasProperty('console')
                def consoleSuffix = withConsole ? '-console' : ''

                def cmd = [
                    'jpackage',
                    '--type', 'app-image',
                    '--dest', outputDir.absolutePath,
                    '--input', stagingDir.absolutePath,
                    '--name', appName,
                    '--main-jar', jarName,
                    '--main-class', mainClass,
                    '--icon', iconPath,
                    '--java-options', '-Xmx1G -Dfile.encoding=UTF-8',
                    '--verbose'
                ]
                if (withConsole) {
                    cmd.add('--win-console')
                    println "   - 模式: Console (带控制台窗口)"
                }

                runCommand(cmd, lwjgl3Project.projectDir)

                println "✅ 应用程序镜像已生成: ${outputDir}/${appName}"
                println "👉 运行 ${outputDir}/${appName}/${appName}.exe 启动游戏 (无需已安装的 JRE)"
            }
        }
    }

    private static void registerPackageSingleExe(Project lwjgl3Project, GdxTasksExtension ext) {
        def rootProject = lwjgl3Project.rootProject

        lwjgl3Project.tasks.register('packageSingleExe') {
            it.group = 'distribution'
            it.description = '使用 Enigma Virtual Box 将 App Image 打包为单文件 Exe'
            it.dependsOn 'jpackageExe'

            it.doLast {
                def rootDir = rootProject.projectDir

                // 查找 Enigma Virtual Box
                def evbPath = null
                def localPropsFile = rootProject.file('local.properties')
                if (localPropsFile.exists()) {
                    Properties p = new Properties()
                    p.load(new FileInputStream(localPropsFile))
                    evbPath = p.getProperty('enigmavb.path')
                }

                if (!evbPath) {
                    def possiblePaths = [
                        "${rootDir}/tools/EnigmaVirtualBox/enigmavbconsole.exe",
                        "C:/Program Files/Enigma Virtual Box/enigmavbconsole.exe",
                        "C:/Program Files (x86)/Enigma Virtual Box/enigmavbconsole.exe",
                        "D:/Program Files/Enigma Virtual Box/enigmavbconsole.exe",
                        "E:/WorkApps/Enigma Virtual Box/enigmavbconsole.exe"
                    ]
                    for (String path : possiblePaths) {
                        if (lwjgl3Project.file(path).exists()) {
                            evbPath = path
                            break
                        }
                    }
                }

                def appName = rootProject.name
                def projectVersion = rootProject.findProperty('projectVersion')
                def withConsole = rootProject.hasProperty('console')
                def consoleSuffix = withConsole ? '-console' : ''
                def distDir = lwjgl3Project.file("${rootDir}/outputs/jpackage/${appName}")
                def outputExe = lwjgl3Project.file("${rootDir}/outputs/${appName}_V${projectVersion}${consoleSuffix}.exe")

                if (!evbPath || !lwjgl3Project.file(evbPath).exists()) {
                    println "\n⚠️ [警告] 无法执行单文件打包"
                    println "请在 local.properties 中配置 'enigmavb.path' 指向 enigmavbconsole.exe"
                    return
                }

                def inputExe = new File(distDir, "${appName}.exe")
                if (!inputExe.exists()) {
                    throw new GradleException("找不到 jpackage 生成的主程序: ${inputExe}")
                }

                println "📝 正在动态生成 Enigma Virtual Box 项目文件..."

                // 递归生成文件列表
                Closure generateFiles
                generateFiles = { builder, File currentDir ->
                    currentDir.eachFile { f ->
                        if (f.absolutePath == inputExe.absolutePath) return

                        if (f.isDirectory()) {
                            builder.File {
                                Type('3')
                                Name(f.name)
                                Action('0')
                                OverwriteDateTime('False')
                                OverwriteAttributes('False')
                                HideFromDialogs('0')
                                Files {
                                    generateFiles(builder, f)
                                    if (f.name == 'app') {
                                        def defaultCfg = new File(f, "${appName}.cfg")
                                        if (defaultCfg.exists()) {
                                            def versionedCfgName = "${appName}_V${projectVersion}${consoleSuffix}.cfg"
                                            println "   - [Fix] 添加配置文件映射: ${versionedCfgName} -> ${defaultCfg.name}"
                                            builder.File {
                                                Type('2')
                                                Name(versionedCfgName)
                                                File(defaultCfg.absolutePath)
                                                ActiveX('False')
                                                ActiveXInstall('False')
                                                Action('0')
                                                OverwriteDateTime('False')
                                                OverwriteAttributes('False')
                                                PassCommandLine('False')
                                                HideFromDialogs('0')
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            builder.File {
                                Type('2')
                                Name(f.name)
                                File(f.absolutePath)
                                ActiveX('False')
                                ActiveXInstall('False')
                                Action('0')
                                OverwriteDateTime('False')
                                OverwriteAttributes('False')
                                PassCommandLine('False')
                                HideFromDialogs('0')
                            }
                        }
                    }
                }

                def tempEvb = lwjgl3Project.file("${rootDir}/temp_build.evb")
                tempEvb.withWriter('UTF-8') { writer ->
                    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n")
                    def xml = new groovy.xml.MarkupBuilder(writer)
                    xml.EnigmaVirtualBox {
                        InputFile(inputExe.absolutePath)
                        OutputFile(outputExe.absolutePath)
                        Files {
                            Enabled('True')
                            DeleteExtractedOnExit('False')
                            CompressFiles('False')
                            Files {
                                File {
                                    Type('3')
                                    Name('%DEFAULT FOLDER%')
                                    Action('0')
                                    OverwriteDateTime('False')
                                    OverwriteAttributes('False')
                                    HideFromDialogs('0')
                                    Files {
                                        generateFiles(xml, distDir)
                                    }
                                }
                            }
                        }
                    }
                }

                def cmd = [evbPath, tempEvb.absolutePath]
                runCommand(cmd, lwjgl3Project.projectDir)
                tempEvb.delete()

                println "✅ 单文件打包完成！"
                println "👉 文件已生成: ${outputExe}"
            }
        }
    }

    /**
     * 执行外部命令并实时打印输出 (默认 GBK 编码, 兼容 Windows)
     */
    static void runCommand(List<String> command, File workingDir, String charset = 'GBK') {
        def safeCommand = command.collect { it.toString() }
        println "Executing: ${safeCommand.join(' ')}"
        def pb = new ProcessBuilder(safeCommand)
        pb.directory(workingDir)
        pb.redirectErrorStream(true)
        def process = pb.start()
        process.outputStream.close()

        try {
            process.inputStream.eachLine(charset) { line -> println line }
        } catch (Exception e) {
            println "Error reading output: ${e.message}"
        }

        process.waitFor()
        if (process.exitValue() != 0) {
            throw new GradleException("Command failed with exit code ${process.exitValue()}")
        }
    }
}
