## Spring Boot Mule 4 Runtime CE

Just a base docker image to run Mule Runtime as Spring boot application using [Spring boot starter for Mule 4](https://github.com/hawkore/mule4-spring-boot-starter).

In this example, it will allow us to easy run our Mule Applications as micro-services, and remote management Mule Runtime (metrics, log file, log levels...).

```yaml
...
  containers:
    - image: docker.hawkore.com/k8s/spring-boot-mule4-runtime-ce:latest
      imagePullPolicy: "Never"
      name: mule-api-app
      env:
        - name: LANG
          value: 'C.UTF-8'
        - name: USER_JVM_OPTS
          value: '-server -Xms128M -Xmx256M
          -DIGNITE_QUIET=false
          -Dspring.application.name=mule-api-app
          -Dmule.apps=file:/opt/mule/shared/mule-api-app-1.0.0-mule-application.jar -Dmule.cleanStartup=true'
...
```

See [statefulSet configuration yaml for REST Api](../kubernetes/6-statefulset-mule-api-app.yaml) or [statefulSet configuration yaml for Worker](../kubernetes/7-statefulset-mule-worker-app.yaml).

# Build

Build docker image (`docker.hawkore.com/k8s/spring-boot-mule4-runtime-ce:latest`):

``` bash
mvn clean install -Pdocker
```
