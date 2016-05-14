# jse-chunk

This application provides a Java batch example based on Java SE. It is a chunk
oriented demo. Chunk oriented processing reders to reading the data one item
at a time, and creating _chunks_ that will be written out, within a transaction
boundary.

## How to run

You need a MySQL database and create the required data before running this demo.

Once finished, run it using maven :

```sh
mvn clean install
mvn exec:java -Dexec.mainClass="App"
```
