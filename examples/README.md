This directory contains examples for pmemkv-java.

### StringExample

It's a basic example, where both (key and value) are String objects.

### ByteBufferExample

It's an example, where key and values are a binary data.

### MixedTypesExample 
As in the name - it has mixed types: keys are String objects and values are binary data.

### PicturesExample

It's more complex one - using persistent engine (cmap) to store pictures in pmemkv. Keys are Strings, values are BufferedImage.

Build:

```sh
cd examples
javac -cp ../src/main/target/*.jar StringExample.java
```

Load pictures to pmemkv database and display it:

```sh
PmemkvPath=/dev/shm/file PmemkvSize=10000000 InputDir=/path/to/directory/with/png/files PMEM_IS_PMEM_FORCE=1 java -ea -Xms1G -cp .:`find ../src/main/target -name *.jar` -Djava.library.path=../src/main/cpp/target PicturesExample
```

Display pictures already stored in pmemkv database:

```sh
PmemkvPath=/dev/shm/file PMEM_IS_PMEM_FORCE=1 java -ea -Xms1G -cp .:`find ../src/main/target -name *.jar` -Djava.library.path=../src/main/cpp/target PicturesExample
```

