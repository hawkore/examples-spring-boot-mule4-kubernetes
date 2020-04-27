# Spring Boot Admin server

To enable remote management for Spring Boot applications on Kubernetes, follow below steps:

1. Add below maven dependencies to your Spring Boot Admin server application, see [pom.xml](pom.xml):
    ```
    <dependency>
      <groupId>de.codecentric</groupId>
      <artifactId>spring-boot-admin-starter-server</artifactId>
      <version>${spring-admin-server.version}</version>
    </dependency>
   
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-kubernetes-all</artifactId>
    </dependency>
    ``` 

2. Add `@EnableAdminServer`, `@EnableDiscoveryClient` and `@EnableScheduling` annotations
to your main Spring Boot Admin Server application class:

    ``` java
    /**
     * SpringBootAdminServer
     *
     * @author Manuel Núñez Sánchez (manuel.nunez@hawkore.com)
     */
    @SpringBootApplication
    @EnableAdminServer
    @EnableDiscoveryClient
    @EnableScheduling
    public class SpringBootAdminServer {
    
        /**
         * The entry point of application.
         *
         * @param args
         *     the input arguments
         */
        public static void main(String[] args) {
            SpringApplication app = new SpringApplication(SpringBootAdminServer.class);
            app.setBannerMode(Mode.OFF);
            app.run(args);
        }
    
    }
    ```
   
3. Create a Kubernetes ConfigMap for Spring Boot Admin Server to tell how to find Spring Boot applications to manage:

    **IMPORTANT**: In our case, the name of this ConfigMap must be equals to `sb-admin-server`, as defined in [application.properties](src/main/resources/application.properties) `spring.application.name` property.

    ```yaml
    # Config Map for Spring Boot Admin Server to auto-discover Spring Boot applications to manage
    apiVersion: v1
    kind: ConfigMap
    metadata:
      name: sb-admin-server
      namespace: my-mule4-stack
    data:
      application.yml: |-
        spring:
         cloud:
          kubernetes:
            discovery:
              all-namespaces: true
              service-labels:
                spring-boot-managed: true
    ```

4. Kubernetes services grouping Spring Boot applications to manage must include label `spring-boot-managed: "true"`, for example:

    ```yaml
    # Service (Load balancer) for Quiz REST Api: api endpoints, RAML console and Spring Boot management (actuator)
    apiVersion: v1
    kind: Service
    metadata:
      name: mule-api-app-service
      namespace: my-mule4-stack
      labels:
        app: mule-api-app
        spring-boot-managed: "true"
    spec:
      ports:
        - name: "mule-http"
          port: 8081
          targetPort: 8081
        - name: "management"
          port: 8888
          protocol: TCP
      selector:
        app: mule-api-app
    ```

## Spring Boot Admin in Cluster - ignite Configuration for Kubernetes

Clustering Spring Boot Admin Server instances requires to provide a mechanism to share information about Spring Boot application instances across Spring Boot Admin Server nodes,
so, as we want to work with Apache Ignite we have implemented a `NotificationTrigger` to deduplicate notifications in cluster and an `EventeStore` backed by a distributed Apache Ignite cache, 
see `org.hawkore.springframework.boot.admin.cluster.IgniteNotificationTrigger` and `org.hawkore.springframework.boot.admin.cluster.IgniteEventStore` implementations for more details:

```java
@Bean
public IgniteEventStore eventStore(@Autowired Ignite ignite,
    @Value("${spring.boot.admin.server.max_events_per_aggregate:100}") int maxLogSizePerAggregate) {
    CacheConfiguration<String, List<InstanceEvent>> config = new CacheConfiguration<>();
    config.setName(nameEventStore);
    config.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
    config.setCacheMode(CacheMode.REPLICATED);
    return new IgniteEventStore(maxLogSizePerAggregate, ignite.getOrCreateCache(config));
}

@Bean(initMethod = "start", destroyMethod = "stop")
@ConditionalOnMissingBean(NotificationTrigger.class)
public NotificationTrigger notificationTrigger(@Autowired Ignite ignite,
    @Autowired Notifier notifier,
    @Autowired Publisher<InstanceEvent> events) {
    CacheConfiguration<String, Long> config = new CacheConfiguration<>();
    config.setName(nameNotificationStore);
    config.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
    config.setCacheMode(CacheMode.REPLICATED);
    // we dont want to preserve old notifications for a long time...
    // so set an expiry policy to auto remove them after a time
    config.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.TEN_MINUTES));
    return new IgniteNotificationTrigger(notifier, events, ignite.getOrCreateCache(config));
}

```

Configure IP finder on [ignite-config.xml](src/main/resources/ignite-config.xml) as `org.apache.ignite.spi.discovery.tcp.ipfinder.kubernetes.TcpDiscoveryKubernetesIpFinder` with the **kubernetes service name** to find other Spring Boot Admin server nodes and the **namespace**.

```xml
 <bean id="ignite-config" class="org.apache.ignite.configuration.IgniteConfiguration">
    ...

    <!-- Explicitly configure TCP discovery SPI -->
    <property name="discoverySpi">
      <bean class="org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi">
        <property name="ipFinder">
          <!--
          Enables Kubernetes IP finder and set namespace and service name (cluster) to find SERVER nodes.
          -->
          <bean class="org.apache.ignite.spi.discovery.tcp.ipfinder.kubernetes.TcpDiscoveryKubernetesIpFinder">
            <property name="shared" value="true" />
            <property name="namespace" value="my-mule4-stack" />
            <property name="serviceName" value="sb-admin-server-service" />
          </bean>
        </property>
      </bean>
    </property>

    ...
</bean>
```

## Kubernetes artifacts

- Namespace `my-mule4-stack`, service `sb-admin-server-service` and ConfigMap `sb-admin-server` for discovery, spring management and load balancing are defined in [k8s configuration yaml for mandatory artifacts](../kubernetes/1-mandatory.yaml)
- [StatefulSet for Spring Boot Admin Server configuration yaml](../kubernetes/3-statefulset-sb-admin-server.yaml)


## Build

Build docker image (`docker.hawkore.com/k8s/spring-boot-admin-server:latest`):

```bash
mvn clean install -Pdocker
```
