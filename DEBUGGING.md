# [Debuging jni with gdb](https://medium.com/@pirogov.alexey/gdb-debug-native-part-of-java-application-c-c-libraries-and-jdk-6593af3b4f3f)

* Build jni with debug symbols

Add debug compilation flag in src/main/cpp/pom.xml in compilerEndOptions section
```xml
 <build>
        <plugins>
           <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>native-maven-plugin</artifactId>
                <compilerEndOptions>
                  <compilerEndOption>-g</compilerEndOptions>
                </compilerEndOptions>
            <plugin>
         <plugins>
<build>
```

* It may be needed to disable [ptrace security options](https://www.kernel.org/doc/Documentation/security/Yama.txt)

```sh
echo 0 > /proc/sys/kernel/yama/ptrace_scope
```

* let's debug basic example:

```sh
gdb --args java -ea -Xms1G -cp .:../target/pmemkv-0.9.0.jar -Djava.library.path=../build BasicExample
(gdb) handle SIGSEGV nostop noprint pass  <- JVM is handling segfault on it's own, so need to disable it in gdb
(gdb) break jni_function_to_debug
```

# Debuging with jdb

Build example with debug information

```sh
javac -g -cp ../target/*.jar BasicExample.java
jdb  -classpath .:../target/pmemkv-0.9.0.jar -Djava.library.path=../build BasicExample
```

# Generating jni header

```sh
javac -h src -cp target/pmemkv-0.9.0.jar src/main/java/io/pmem/pmemkv/Database.java 
```
