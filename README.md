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

    mvn clean install


[badge-build]: https://img.shields.io/badge/build-failed-red.svg
[badge-license]: https://img.shields.io/badge/license-Apache2.0-brightgreen.svg
[home]: https://github.com/mincong-h/gsoc-hsearch