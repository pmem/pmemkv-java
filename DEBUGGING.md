# [Debugging jni with gdb](https://medium.com/@pirogov.alexey/gdb-debug-native-part-of-java-application-c-c-libraries-and-jdk-6593af3b4f3f)

* Build jni with debug symbols

Add debug compilation flag in jni-binding/pom.xml in compilerEndOptions section

```xml
<!-- in native-maven-plugin -->
<compilerEndOptions>
  <compilerEndOption>-g</compilerEndOptions>
</compilerEndOptions>
```

* It may be needed to disable [ptrace security options](https://www.kernel.org/doc/Documentation/security/Yama.txt)

```sh
echo 0 > /proc/sys/kernel/yama/ptrace_scope
```

* let's debug basic example:

```sh
gdb --args java -ea -Xms1G -jar MixedTypesExample/target/MixedTypesExample-1.0.0-jar-with-dependencies.jar
(gdb) handle SIGSEGV nostop noprint pass  <- JVM is handling segfault on its own, so need to disable it in gdb
(gdb) break jni_function_to_debug
```

# Debuging with jdb

Build example with debug information

```sh
cd MixedTypesExample/target
javac -g -cp target/*jar-with-dependencies.jar src/main/java/MixedTypesExample.java
jdb -classpath MixedTypesExample-1.0.0-jar-with-dependencies.jar MixedTypesExample
```

# Generating jni header

```sh
javac -h jni-binding/ -cp pmemkv-binding/target/pmemkv-1.0.0.jar pmemkv-binding/src/main/java/io/pmem/pmemkv/Database.java
```
