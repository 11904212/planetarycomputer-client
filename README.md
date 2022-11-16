Planetary Computer Client
==============

A Java client for interacting with the [Microsoft Planetary Computer](https://planetarycomputer.microsoft.com/).

The main task of the client is to sign the resources provided by the Planetary Computer 
with a [SAS-token](https://planetarycomputer.microsoft.com/docs/concepts/sas/) in order to make them retrievable.
For this purpose, a [STAC-client](https://github.com/11904212/java-stac-client) is extended to automatically sign each retrieved asset.


```java
PlanetaryComputerClient pcClient = new PCClientImpl();

QueryParameter parameter = new QueryParameter();
parameter.addCollection("sentinel-2-l2a");
parameter.setDatetime("2022-02-13/2022-04-15");

ItemCollection result = pcClient.search(parameter);
```


## Using Planetary Computer Client
### Maven dependency ###

* Declare the dependency
```
<dependency>
  <groupId>io.github.11904212</groupId>
  <artifactId>planetarycomputer-client</artifactId>
  <version>alpha.0.3</version>
</dependency>
```


### Remote Dependencies ###

* [Java STAC client](https://github.com/11904212/java-stac-client) (Apache License, Version 2.0) - A simple STAC client written in Java
* [Jackson Data Processor](https://github.com/FasterXML/jackson-databind) (Apache License, Version 2.0) - Jackson data-binding functionality and tree-model
