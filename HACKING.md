# Debuging jni with gdb

* Build jni with debug symbols
```
TBD How to do it
```

* It may be needed to disable ptrace security options to debug
```
echo 0 > /proc/sys/kernel/yama/ptrace_scope
```
* lets debug basic example:

```
gdb --args java -ea -Xms1G -cp .:../target/pmemkv-0.9.0.jar -Djava.library.path=../build BasicExample
(gdb) handle SIGSEGV nostop noprint pass  <- JVM is handling segfault on it's own, so need to disable it in gdb
(gdb) break jni_function_to_debug
```

# Debuging with jdb

Build example with debug informations
```
javac -g -cp ../target/*.jar BasicExample.java
jdb  -classpath .:../target/pmemkv-0.9.0.jar -Djava.library.path=../build BasicExample
```

# Generating jni header
```
javac -h src -cp target/pmemkv-0.9.0.jar src/main/java/io/pmem/pmemkv/Database.java 
```
# Bibliography
* https://medium.com/@pirogov.alexey/gdb-debug-native-part-of-java-application-c-c-libraries-and-jdk-6593af3b4f3f
* https://www.kernel.org/doc/Documentation/security/Yama.txt
