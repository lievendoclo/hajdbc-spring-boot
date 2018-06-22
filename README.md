![](https://api.travis-ci.org/lievendoclo/hajdbc-spring-boot.svg?branch=develop)
![](https://img.shields.io/badge/spring%20boot%202-compatible-green.svg)

# hajdbc-spring-boot

Autoconfiguration of HA-JDBC components for Spring Boot



# Usage

Just include this project as as dependency in your Spring Boot project and add the configuration
for the backend databases in your application properties.

## YAML configuration
```
hajdbc:
    driverDatabases:
        - id: db1
          location: jdbc:hsqldb:mem:db1
          user: sa
        - id: db1
          location: jdbc:hsqldb:mem:db1
          user: sa
spring:
    datasource:
        url: jdbc:ha-jdbc:default
        username: sa
```

## Properties file configuration
```
hajdbc.driverDatabases[0].id=db1
hajdbc.driverDatabases[0].location=jdbc:hsqldb:mem:db1
hajdbc.driverDatabases[0].user=sa
hajdbc.driverDatabases[1].id=db2
hajdbc.driverDatabases[1].location=jdbc:hsqldb:mem:db2
hajdbc.driverDatabases[1].user=sa

spring.datasource.url=jdbc:ha-jdbc:default
spring.datasource.username=sa
```

## Overriding default HA-JDBC factories

By default, this integration uses default factory implementations for the state manager,
database metadata cache and balancer. If you provide your own implementation as a bean,
the integration will pick those over the default. If for example you want to use a SQLite
state manager, just create a bean like this in your configuration:

```
@Bean
public StateManagerFactory stateManagerFactory() {
    return new SQLiteStateManagerFactory();
}
```

By default. the integration will use a SimpleStateManagerFactory, a RoundRobinBalancerFactory
and a SharedEagerDatabaseMetaDataCacheFactory.

## Configurable properties

All configurable properties are prefixed with hajdbc. The following properties are configurable for this integration:

- **enabled**: Whether HA JDBC should be enabled. Default is true
- **driverDatabases**: The backend databases for HA JDBC. Required to be configured.
- **clusterName**: The name of the HA JDBC cluster, used in the ha-jdbc jdbc url. Default is "default"
- **cronExpression**: The cron expression that indicates when synchronization should occur
- **defaultSynchronizationStrategy**: Which synchronization strategy should be chosen. Possible values are
"full", "fastdiff", "diff", "dump-restore", "per-table" and "passive". Default is "full"
- **enableIdentityColumnDetection**: Whether identity column detection is enabled. Default is true
- **enableSequenceDetection**: Whether sequence detection is enabled. Default is true

