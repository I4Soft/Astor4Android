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

2. Run the script inside the astor4android main directory:

	`./run_example.sh`  

## Execution

In Astor4Android, you have the following command line arguments:

| Argument | Description |
| --- | --- |
| location | Location of the Android project to repair. The project must be clean. You can clean it using the command `./gradlew clean` inside the project's directory. |
| mode | Mode of execution. More information in the next tables. |
| flmode | (Optional) Fault localization mode. More information in the next tables. |
| loadflsave | (Optional) File containing the results of a previous fault localization run for the same project listed at the "location" argument. This file is created at the working directory (e.g. astor4android/outputMutation/Astor4AndroidMain-Project) after a fault localization is executed on a project and has the extension ".flsave". If you're gonna use the same project multiple times, it's useful to save this file and use this argument to load it. |
| flthreshold | Minimun suspicious value for a line during fault localization (Number between 0 and 1). If you set a value that is too high, Astor4Android will prompt an option for you to set a new value at runtime. |
| androidsdk | Location of the Android SDK folder. Usually this argument is set to $ANDROID_HOME. |
| jvm4testexecution | Location of the java executable. Usually set to %JAVA_HOME/bin. |
| javacompliancelevel | Compliance level of the Java source code. The recommended value is 8. |
| stopfirst | Determines if the execution should be stopped after the first fix (true of false). |
| unitfailing | Failing JUnit test cases separated by a classpath separator (":" on Linux/Mac, ";" on Windows). A test case is the fully qualified class name of the test followed by a #, followed by the method's name. (e.g. com.example.root.bugapp2.ExampleUnitTest#multy) |
| instrumentationfailing | Failing instrumentation test cases separated by a classpath separator (":" on Linux/Mac, ";" on Windows). A test case is the fully qualified class name of the test followed by a #, followed by the method's name. (e.g. com.example.root.bugapp2.ExampleInstru#use) |
| port | Port that all AstorWorkers will connect to. |


For the argument "mode", there are four options:

| Mode | Description |
| --- | --- |
| statement | Executes using JGenProg |
| statement-remove | Executes using JKali |
| mutation | Executes using JMutRepair |
| custom | Executes using a custom engine |


For the argument "flmode", there are five options:

| Mode | Description |
| --- | --- |
| ochiai | Uses the Ochiai method. (Default) |
| op2 | Uses the Op2 method. |
| tarantula | Uses the Tarantula method. |
| barinel | Uses the Barinel method. |
| dstar | Uses the DStar method with * = 2. |


To run Astor4Android, follow these instructions:  

1. Run at least one [AstorWorker](https://github.com/kayquesousa/astorworker).

2. Run this command (replacing `<arguments>` with the actual arguments):

   				mvn exec:java -Dexec.mainClass=br.ufg.inf.astor4android.main.evolution.Astor4AndroidMain -Dexec.args="<arguments>"

   Example:  

			mvn exec:java -Dexec.mainClass=br.ufg.inf.astor4android.main.evolution.Astor4AndroidMain -Dexec.args="-mode statement -location /home/astor4android/Examples/BugApp2 -androidsdk $ANDROID_HOME -jvm4testexecution $JAVA_HOME/bin  -javacompliancelevel 8 -stopfirst true  -flthreshold 0.9  -unitfailing com.example.root.bugapp2.ExampleUnitTest#multy -instrumentationfailing com.example.root.bugapp2.ExampleInstrumentedTest#useAppContext -port 6665"
  
