# Worker Mule Application

Workers will listen on a distributed Queue for new Quiz to process:
- Avoiding more than one Quiz per surveyed (**distributed LOCK scope**).
- Updating Quiz with additional data.
- Storing Quiz into Apache Ignite's **distributed database** (SQL Query Entity).
- Updating global stats.

![kube-mule-ignite-worker](../docs/assets/kube-mule-ignite-worker.gif)

Mule flow to listen on distributed Queue for new Quiz to process, ensuring uniqueness:

![mule-worker-app-ingest-flow](../docs/assets/mule-worker-app-ingest-flow.png)

Mule flow to process Quizzes:

![mule-worker-app-process-flow](../docs/assets/mule-worker-app-process-flow.png)

## Ignite Configuration for kubernetes

Configure IP finder on [ignite-config.xml](src/main/resources/ignite-config.xml) as `org.apache.ignite.spi.discovery.tcp.ipfinder.kubernetes.TcpDiscoveryKubernetesIpFinder` with the **kubernetes service name** to find server nodes and the **namespace**.

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

- Namespace `my-mule4-stack` and service `ignite-cluster-one-service` and service `mule-worker-app-service` for discovery and spring management are defined in [k8s configuration yaml for mandatory artifacts](../kubernetes/1-mandatory.yaml)
- Will be started as a micro-service using [Spring Boot Mule 4 Runtime CE docker image](../spring-boot-mule4-runtime-ce/README.md), see [StatefulSet configuration yaml for Worker](../kubernetes/7-statefulset-mule-worker-app.yaml)

# Build Mule application

Build application:

```bash
mvn clean package
```

