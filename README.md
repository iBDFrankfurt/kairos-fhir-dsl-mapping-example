Example project for the Kairos FHIR DSL
========================================
a.k.  kairos-fhir-dsl-mapping-example

## Scope

* This project contains Groovy example scripts for the use of the FHIR custom export interface. More infos to CentraXX can be found on
the [Kairos Website](https://www.kairos.de/en/)

## Requirements

* To write or modify custom export scripts, it is necessary to have a very good understanding of the source and target data models to transform into
  each other. Therefore, it is very helpful to use the kairos-fhir-dsl library as a dependency, which contains a CentraXX JPA meta model as a source,
  and the FHIR R4 model as a target.
* This project uses [Maven](https://maven.apache.org/) for build management to download all necessary dependencies
  from [Maven Central](https://mvnrepository.com/repos/central) or the kairos-fhir-dsl library
  from [GitHub Packages](https://github.com/kairos-fhir/kairos-fhir-dsl-mapping-example/packages).

## GitHub Authentication with Maven

* Because GitHub does not allow downloading packages without access token, use maven with the access token in the local [settings.xml](settings.xml)
  in this project.

  ```
  mvn install -s settings.xml
  ```

* IntelliJ user can override the user settings file by File -> Settings -> Build Tools -> Maven or create own Maven run configurations. It is also
  possible to add it to .mvn/maven.config or to copy and past the repository authentication to another existing settings.file

* The kairos-fhir-dsl binaries before v.1.5.0 have not been published on a public maven repository yet, but can be downloaded in
  the [assets section of the corresponding tag](https://github.com/kairos-fhir/kairos-fhir-dsl-mapping-example/releases)
  and [installed manually](https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html).

## Versioning

* The versioning of this example projects will be parallel to the kairos-fhir-dsl library, which
  follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
* All Groovy example scripts will contain a @since annotation that describes the first CentraXX version, that can interpret the respective script. The
  specified CentraXX version contains the necessary minimal version of the kairos-fhir-dsl library, CXX entity exporter, initializer and support for
  the ExportResourceMappingConfig.json.

## How-To

Further instructions to the interface and its DSL can be found in the [German how-to](/CXX_FHIR_Custom_Export.pdf).
