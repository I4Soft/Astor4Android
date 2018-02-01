# Astor4Android
Astor4Android is an adaptation of the original [Astor](https://github.com/SpoonLabs/astor) to work with Android applications.

Contacts:
Kayque de S. Teixeira - kayque23@gmail.com
Celso G Camilo-Junior - celsocamilo@gmail.com


## Compilation

1. Install Android SDK, JDK 1.8 and Maven.

2. Set the environment variables ANDROID_HOME and JAVA_HOME.  
	
	Example:  
	`export ANDROID_HOME=/home/kayquesousa/Android/Sdk`  
	`export JAVA_HOME=/home/kayquesousa/jdk1.8.0_131`  

4. Clone the Astor4Android repository and compile it using Maven:
	
	`git clone https://github.com/kayquesousa/astor4android.git`  
	`cd astor4android`  
	`mvn clean compile`  

## Test (Optional)

1. Run at least one [AstorWorker](https://github.com/kayquesousa/astorworker).

2. Build dependencies using Maven and create a file containing their locations separated by a colon:  
	
	`mvn  dependency:build-classpath`  
	`mvn  dependency:build-classpath | egrep -v "(^\[INFO\]|^\[WARNING\])" | tee astor-classpath.txt`  

	You can use the same astor-classpath.txt for future executions.  

3. Run the script inside the astor4android main directory:

	`./run_example.sh`  

## Execution

In Astor4Android, you have the following command line arguments:

| Argument | Description |
| --- | --- |
| location | Location of the Android project to repair. The project must be clean. You can clean it using the command `./gradlew clean` inside the project's directory. |
| mode | Mode of execution. More information on the next table. |
| androidjar | Location of the android.jar. android.jar is usually found at $ANDROID_HOME/platforms/android-VERSION/android.jar), where VERSION is a number. |
| androidsdk | Location of the Android SDK folder. Usually this argument is set to $ANDROID_HOME. |
| jvm4testexecution | Location of the java executable. Usually set to %JAVA_HOME/bin. |
| javacompliancelevel | Compliance level of the source code. 8 is the recommended value. |
| stopfirst | Determines if the execution should be stopped after the first fix (true of false). |
| flthreshold | Minimun suspicious value for fault localization (Number between 0 and 1). |
| unitfailing | Failing Junit tests separated by a colon. |
| instrumentationfailing | Failing instrumentation tests separated by a colon. |
| port | Port that all AstorWorkers will connect on. |


For the argument "mode", there are four options:

| Mode | Description |
| --- | --- |
| statement | Executes using JGenProg |
| statement-remove | Executes using JKali |
| mutation | Executes using JMutRepair |
| custom | Executes using a custom engine |

To execute Astor4Android, follow these instructions:  

1. Run at least one [AstorWorker](https://github.com/kayquesousa/astorworker).

2. Build dependencies using Maven and create a file containing their locations separated by a colon:  
	
	`mvn  dependency:build-classpath`  
	`mvn  dependency:build-classpath | egrep -v "(^\[INFO\]|^\[WARNING\])" | tee astor-classpath.txt`  

	You can use the same astor-classpath.txt for future executions.  

3. Run the command  

   				java -cp $(cat astor-classpath.txt):target/classes br.ufg.inf.main.evolution.Astor4AndroidMain 
   				
   followed by all the other arguments.  

   Example:  

			java -cp $(cat astor-classpath.txt):target/classes br.ufg.inf.main.evolution.Astor4AndroidMain -mode statement -location $(pwd)/Examples/Simple-Calculator -androidsdk $ANDROID_HOME -androidjar $ANDROID_HOME/platforms/android-25/android.jar -jvm4testexecution $JAVA_HOME/bin  -javacompliancelevel 8 -stopfirst true  -flthreshold 0.9  -instrumentationfailing com.simplemobiletools.calculator.MainActivityTest#rootTest:com.simplemobiletools.calculator.MainActivityTest#complexTest -port 6665  
