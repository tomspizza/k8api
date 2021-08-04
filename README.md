## K8 API
The APIs support for deployment services to kubernetes environment.

### Prerequisite
The project requires java version 11 for running.

### Build project
```shell
mvn clean install
```

### Start
```shell
java -jar -Djdk.tls.client.protocols=TLSv1.2 target/k8api-1.0.0.jar
```

### Testing
Access swagger: http://localhost:8080/swagger-ui.html