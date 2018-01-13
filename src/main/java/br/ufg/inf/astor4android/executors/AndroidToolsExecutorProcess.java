package br.ufg.inf.astor4android.executors;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;


/**
 * 
 * @author Kayque de Sousa Teixeira
 *
 */
public class AndroidToolsExecutorProcess {
	private static String GRADLE;
	private static String ADB;
	private static String ANDROID_HOME;

	public static void setup(String androidHome) throws Exception {
		ANDROID_HOME = androidHome;

		switch(getOperatingSystem()){
			case "Windows":
				GRADLE = "cmd /c gradlew.bat";
				ADB = "adb";
				break;

			case "Unix":
			case "MacOS":
				GRADLE = "./gradlew";
				ADB = "./adb";
				break;
		}
	}

	public static void compileProject(String projectLocation) throws Exception {
		CommandExecutorProcess.execute(GRADLE + " build -x test -no-daemon", projectLocation);
	}	
	
	public static List<String> runGradleTask(String projectLocation, String gradleTask) throws Exception  {
		List<String> output = CommandExecutorProcess.execute(GRADLE + " " + gradleTask + " -no-daemon", projectLocation);

		// TODO: find a way to check successfulness
		return output;
	}

	public static String getOperatingSystem() {
		String operatingSystem = System.getProperty("os.name").toLowerCase();

		if(operatingSystem.contains("win")) return "Windows";
		if(operatingSystem.contains("nux") ||
			operatingSystem.contains("aix") ||
			operatingSystem.contains("nix")) return "Unix";

		if(operatingSystem.contains("mac")) return "MacOS";

		return null;
	}

	public static String getAndroidHome() {
		return ANDROID_HOME;
	}
}
