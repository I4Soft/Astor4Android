cd Examples/Simple-Calculator
printf "sdk.dir=$ANDROID_HOME\nndk.dir=$ANDROID_NDK_HOME" | tee local.properties
cd ../..
java -cp $(cat astor-classpath.txt):target/classes br.ufg.inf.main.evolution.Astor4AndroidMain -mode statement -location $(pwd)/Examples/Simple-Calculator -androidsdk $ANDROID_HOME -androidjar $ANDROID_HOME/platforms/android-25/android.jar -jvm4testexecution $JAVA_HOME/bin  -javacompliancelevel 8 -stopfirst true  -flthreshold 0.9  -instrumentationfailing com.simplemobiletools.calculator.MainActivityTest#rootTest:com.simplemobiletools.calculator.MainActivityTest#complexTest -port 6665
