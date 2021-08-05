## K8 API
The APIs support for deployment services to kubernetes environment.

### Prerequisite
- The project requires java version 11 for running.
- Make sure the kubernetes cluster is installed Nginx ingress controller.

For EKS
```shell
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-0.32.0/deploy/static/provider/aws/deploy.yaml
```

For Digital Oceans
```shell
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v0.34.1/deploy/static/provider/do/deploy.yaml
```

For AKS: https://docs.microsoft.com/en-us/azure/aks/ingress-static-ip

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