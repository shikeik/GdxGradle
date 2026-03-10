package ${projectPackage};

/**
 * 项目构建配置信息
 * 此文件由 Gradle 插件自动生成，请勿手动修改
 */
public class BuildConfig {
	public static final String PROJECT_NAME = "${projectName}";
	public static final String DEV_VERSION = "${projectVersion}";
	public static final String JDK_VERSION = "${jdkVersion}";
	
	// 构建流水号（项目生命周期总构建次数）
	public static final int BUILD_COUNT = ${buildCount};
	public static final String DISPLAY_VERSION = "${projectVersion}#${buildCount}";
	
	// Android 版本号（与 BUILD_COUNT 同步）
	public static final int VERSION_CODE = ${buildCount};
	
	// 构建信息
	public static final long BUILD_TIMESTAMP = ${System.currentTimeMillis()}L;
	public static final String BUILD_TIME = "${new java.util.Date().format('yyyy-MM-dd HH:mm:ss')}";
}
