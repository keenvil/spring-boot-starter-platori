# Keenvil Platori
Platori is intended to encapsulate common internal API Client behavior like
connections configuration, retry policies, error decoding, etc. wrapping Netflix
Feign.

Platori must be used by any module which consumes another module API.

## Prerequisites - Development

You will need the following things properly installed on your computer.

* [Git](http://git-scm.com/)
* [Maven 3.3.9](http://maven.apache.org)
* [Java - Oracle 1.8.0_121](http://java.com)
* [MySQL - 5.7.17](http://www.mysql.com/)

## Installation for developmet

### Getting the code
```
$ git clone https://github.com/my-community/security-api.git`
```

### Running
```
$ mvn clean install
```

## Usage
### Configuration
In your Keenvil module pom file include:
```
    <dependency>
      <groupId>com.keenvil</groupId>
      <artifactId>spring-boot-starter-platori</artifactId>
      <version>${keenvil.platori.version}</version>
    </dependency>
```

Platori exposes its configuration trought the following properties:
```
keenvil:
  platori:
    feign:
      options:
        connection-timeout: 5000
        read-timeout: 5000
        logger-leve: FULL
```

* `connection-timeout` (millis): Client connection timeout,
* `read-timeout` (millis): Client answer timeout,
* `logger-level` (NONE, BASIC, HEADERS, FULL): Logger level.

### Error Decoder
Platori implementes a default Feign `ErrorDecoder` which decodes and translate
HTTP Status calls to `KeenvilApiException`.

### Example code usage
```java

@FeignClient(name = "crowdapi",
    url = "${crowdapi-client.server}:"
      + "${crowdapi-client.port}"
      + "${crowdapi-client.context}/",
      configuration = PlatoriFeignConfiguration.class)
public interface CrowdApiClient {}

```

