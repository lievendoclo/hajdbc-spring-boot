package be.insaneprogramming.springboot.hajdbc;

import net.sf.hajdbc.SimpleDatabaseClusterConfigurationFactory;
import net.sf.hajdbc.SynchronizationStrategy;
import net.sf.hajdbc.balancer.BalancerFactory;
import net.sf.hajdbc.balancer.simple.SimpleBalancerFactory;
import net.sf.hajdbc.cache.DatabaseMetaDataCacheFactory;
import net.sf.hajdbc.cache.eager.SharedEagerDatabaseMetaDataCacheFactory;
import net.sf.hajdbc.sql.Driver;
import net.sf.hajdbc.sql.DriverDatabase;
import net.sf.hajdbc.sql.DriverDatabaseClusterConfiguration;
import net.sf.hajdbc.state.StateManagerFactory;
import net.sf.hajdbc.state.simple.SimpleStateManagerFactory;
import net.sf.hajdbc.sync.DifferentialSynchronizationStrategy;
import net.sf.hajdbc.sync.DumpRestoreSynchronizationStrategy;
import net.sf.hajdbc.sync.FastDifferentialSynchronizationStrategy;
import net.sf.hajdbc.sync.FullSynchronizationStrategy;
import net.sf.hajdbc.sync.PassiveSynchronizationStrategy;
import net.sf.hajdbc.sync.PerTableSynchronizationStrategy;
import net.sf.hajdbc.util.concurrent.cron.CronExpression;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import lombok.Data;

/**
 * Autoconfiguration support for HA-JDBC.
 */
@ConfigurationProperties(prefix="hajdbc")
@ConditionalOnClass(Driver.class)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@Configuration
@Data
public class HaJdbcAutoConfiguration {
    private static final List<SynchronizationStrategy> SYNCHRONIZATION_STRATEGIES = Arrays.asList(
        new FullSynchronizationStrategy(),
        new DumpRestoreSynchronizationStrategy(),
        new DifferentialSynchronizationStrategy(),
        new FastDifferentialSynchronizationStrategy(),
        new PerTableSynchronizationStrategy(new FullSynchronizationStrategy()),
        new PassiveSynchronizationStrategy()
    );

    /**
     * Enable or disable HA JDBC integration
     * **/
    boolean enabled = true;
    /**
     * Configure the backend databases for HA JDBC. Possible properties
     * are id, location, driver, user and password
     */
    List<DriverDatabase> driverDatabases;
    /**
     * The name of the HA JDBC cluster. To be used in the Spring Boot datasource url
     */
    String clusterName = "default";
    /**
     * The cron expression that indicates when synchronization should occur
     */
    String cronExpression = "0 0/1 * 1/1 * ? *";
    /**
     * What synchronization factory should be used
     */
    String defaultSynchronizationStrategy = "full";
    /**
     * Whether identity column detection should be enabled
     */
    boolean identityColumnDetectionEnabled = true;
    /**
     * Whether sequence detection should be enabled
     */
    boolean sequenceDetectionEnabled = true;

    @Bean
    @ConditionalOnMissingBean
    public BalancerFactory balancerFactory() {
        return new SimpleBalancerFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public StateManagerFactory stateManagerFactory() {
        return new SimpleStateManagerFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public DatabaseMetaDataCacheFactory databaseMetaDataCacheFactory() {
        return new SharedEagerDatabaseMetaDataCacheFactory();
    }

    @PostConstruct
    void register() throws ParseException {
        if (enabled) {
            DriverDatabaseClusterConfiguration config = new DriverDatabaseClusterConfiguration();
            if (driverDatabases != null && driverDatabases.size() > 0) {
                config.setDatabases(driverDatabases);
            } else {
                throw new IllegalStateException("HA JDBC driver databases should be configured to contain at least one driver database");
            }
            config.setDatabaseMetaDataCacheFactory(databaseMetaDataCacheFactory());
            config.setBalancerFactory(balancerFactory());
            config.setStateManagerFactory(stateManagerFactory());
            config.setSynchronizationStrategyMap(getSynchronizationStrategyMap());
            config.setDefaultSynchronizationStrategy(defaultSynchronizationStrategy);
            config.setIdentityColumnDetectionEnabled(identityColumnDetectionEnabled);
            config.setSequenceDetectionEnabled(sequenceDetectionEnabled);
            config.setAutoActivationExpression(new CronExpression(cronExpression));

            Driver.setConfigurationFactory(clusterName,
                                           new SimpleDatabaseClusterConfigurationFactory<java.sql.Driver, DriverDatabase>(config));
        }
    }

    private static Map<String, SynchronizationStrategy> getSynchronizationStrategyMap() {
        Map<String, SynchronizationStrategy> map = new HashMap<>();
        for (SynchronizationStrategy synchronizationStrategy : SYNCHRONIZATION_STRATEGIES) {
            map.put(synchronizationStrategy.getId(), synchronizationStrategy);
        }
        return map;
    }
}
