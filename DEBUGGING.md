# Debugging JNI with gdb

As a starting point, we recommend good description of [how to debug JNI code using gdb](https://medium.com/@pirogov.alexey/gdb-debug-native-part-of-java-application-c-c-libraries-and-jdk-6593af3b4f3f).

To build our JNI code with debug symbols - add extra debug compilation
flag in `jni-binding/pom.xml`, in `compilerEndOptions` section:

```xml
<!-- in native-maven-plugin -->
<compilerEndOptions>
  <compilerEndOption>-g</compilerEndOption>
  ...
</compilerEndOptions>
```

It may be needed to disable [ptrace security options](https://www.kernel.org/doc/Documentation/security/Yama.txt):

```sh
echo 0 > /proc/sys/kernel/yama/ptrace_scope
```

Now let's debug basic example:

```sh
cd examples
gdb --args java -ea -Xms1G -jar MixedTypesExample/target/MixedTypesExample-*-jar-with-dependencies.jar
(gdb) handle SIGSEGV nostop noprint pass  <- JVM is handling segfault on its own, so need to disable it in gdb
(gdb) break jni_function_to_debug
```

# Debugging with jdb

Build example with debug information

```sh
cd MixedTypesExample/target
javac -g -cp MixedTypesExample-*-jar-with-dependencies.jar ../src/main/java/MixedTypesExample.java
jdb -classpath MixedTypesExample-*-jar-with-dependencies.jar MixedTypesExample
```

# Generating JNI header(s)

To generate JNI header e.g. for Database class, run:

```sh
javac -h jni-binding/ -cp pmemkv-binding/target/pmemkv-*.jar pmemkv-binding/src/main/java/io/pmem/pmemkv/Database.java
```
