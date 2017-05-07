cd Examples/BugApp2
./gradlew clean -no-daemon
cd ../..
java -cp $(cat astor-classpath.txt):target/classes br.ufg.inf.main.evolution.Astor4AndroidMain -mode statement -location $(pwd)/Examples/BugApp2 -androidjar $ANDROID_HOME/platforms/android-25/android.jar -jvm4testexecution $JAVA_HOME/bin  -javacompliancelevel 8 -stopfirst true  -flthreshold 0.9  -unitfailing com.example.root.bugapp2.ExampleUnitTest -instrumentationfailing com.example.root.bugapp2.ExampleInstrumentedTest -port 6665