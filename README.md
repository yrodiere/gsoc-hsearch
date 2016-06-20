[![badge license][badge-license]][home]
[![badge build][badge-build]][home]

# gsoc-hsearch

This project aims to provide an alternative to the current mass indexer 
implementation, using the Java Batch architecture as defined by JSR 352. This 
standardized tool JSR 352 provides task-and-chunk oriented processing, parallel 
execution and many other optimization features. This batch job should accept 
the entity type(s) to re-index as an input, load the relevant entities from the 
database and rebuild the full-text index from these.

## Run

### Create MySQL database

Before running the project, you need to create a database in mysql server.

```sql
CREATE SCHEMA `gsoc` DEFAULT CHARACTER SET utf8 COLLATE utf8_unicode_ci;
```

And import the example data. (_TODO: add data in Google Drive_)

### Overwrite the data source (optional)

The default user name and password of MySQL DS are described in WF configuration
file, located in sub-system `urn:jboss:domain:datasources:4.0`.

    ./src/wildflyConfig//standalone/configuration/standalone-full-testqueues.xml 

Overwrite them if needed

```xml
<security>
  <user-name>root</user-name>
  <password>root</password>
</security>
```

### install

Now, you're ready to install the project:

    mvn clean install


[badge-build]: https://img.shields.io/badge/build-success-brightgreen.svg
[badge-license]: https://img.shields.io/badge/license-Apache2.0-brightgreen.svg
[home]: https://github.com/mincong-h/gsoc-hsearch