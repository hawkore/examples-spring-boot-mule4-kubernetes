## Spring Boot Apache Ignite Server Node

A simple implementation to run Apache Ignite into a Spring Boot application, see `org.hawkore.springframework.boot.ignite.config.IgniteConfig` for more details.

## Ignite Configuration for kubernetes

Set Apache Ignite node as `server node` (`clientMode=false`), enable persistence and configure IP finder on [ignite-config.xml](src/main/resources/ignite-config.xml) as `org.apache.ignite.spi.discovery.tcp.ipfinder.kubernetes.TcpDiscoveryKubernetesIpFinder` with the **kubernetes service name** to find server nodes and the **namespace**.

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
            <property name="serviceName" value="ignite-cluster-one-service" />
          </bean>
        </property>
      </bean>
    </property>

    ...
</bean>
```

## Kubernetes artifacts

- Namespace `my-mule4-stack` and service `ignite-cluster-one-service` for discovery, spring management and load balancing are defined in [k8s configuration yaml for mandatory artifacts](../kubernetes/1-mandatory.yaml)
- See [StatefulSet configuration yaml for Spring Boot Apache Ignite Server](../kubernetes/4-statefulset-ignite-server-node.yaml)

## Build

Build docker image (`docker.hawkore.com/k8s/spring-boot-apache-ignite-server:latest`):

```bash
mvn clean install -Pdocker
```
