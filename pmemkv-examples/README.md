## Pmemkv-examples
This directory contains examples for pmemkv-java.

Examples can be built by running Maven:
```sh
mvn package
```

### StringExample
It's a basic example, where both (key and value) are String objects.

### ByteBufferExample
It's an example, where key and values are a binary data.

### MixedTypesExample
As in the name - it has mixed types: keys are String objects and values are binary data.

### PicturesExample
It's more complex one - it uses persistent engine (cmap) to store pictures in pmemkv. Keys are String, values are BufferedImage.

Load pictures in the png format from the directory specified by the InputDir environment variable to new pmemkv datastore of size (in bytes) specified by PmemkvSize and display all pictures stored in this datastore.
```sh
PmemkvPath=/dev/shm/file PmemkvSize=10000000 InputDir=/path/to/directory/with/png/files PMEM_IS_PMEM_FORCE=1 java -ea -Xms1G -jar PicturesExample-1.0.0-jar-with-dependencies.jar
```

Display pictures already stored in the pmemkv datastore:
```sh
PmemkvPath=/dev/shm/file PMEM_IS_PMEM_FORCE=1 java -ea -Xms1G -jar PicturesExample-1.0.0-jar-with-dependencies.jar
```

Add new pictures to already existing pmemkv datastore and display all pictures stored in this datastore.
```sh
PmemkvPath=/dev/shm/file InputDir=/path/to/directory/with/png/files PMEM_IS_PMEM_FORCE=1 java -ea -Xms1G -jar PicturesExample-1.0.0-jar-with-dependencies.jar
```


