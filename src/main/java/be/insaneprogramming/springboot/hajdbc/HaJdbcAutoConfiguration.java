package be.insaneprogramming.springboot.hajdbc;

import java.text.ParseException;
import java.util.*;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
import net.sf.hajdbc.util.concurrent.cron.CronExpression;

/**
 * Autoconfiguration support for HA-JDBC.
 */
@ConditionalOnClass(Driver.class)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@EnableConfigurationProperties(HaJdbcProperties.class)
@Configuration
public class HaJdbcAutoConfiguration {
    private HaJdbcProperties properties;

    public HaJdbcAutoConfiguration(HaJdbcProperties properties) {
        this.properties = properties;
    }

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
        if (properties.isEnabled()) {
            DriverDatabaseClusterConfiguration config = new DriverDatabaseClusterConfiguration();
            if (properties.getDriverDatabases() != null && !properties.getDriverDatabases().isEmpty()) {
                config.setDatabases(properties.getDriverDatabases());
            } else {
                throw new IllegalStateException("HA JDBC driver databases should be configured to contain at least one driver database");
            }
            config.setDatabaseMetaDataCacheFactory(databaseMetaDataCacheFactory());
            config.setBalancerFactory(balancerFactory());
            config.setStateManagerFactory(stateManagerFactory());
            config.setSynchronizationStrategyMap(getSynchronizationStrategyMap());
            config.setDefaultSynchronizationStrategy(properties.getDefaultSynchronizationStrategy());
            config.setIdentityColumnDetectionEnabled(properties.isIdentityColumnDetectionEnabled());
            config.setSequenceDetectionEnabled(properties.isSequenceDetectionEnabled());
            config.setAutoActivationExpression(new CronExpression(properties.getCronExpression()));

            Driver.setConfigurationFactory(properties.getClusterName(),
                                           new SimpleDatabaseClusterConfigurationFactory<>(config));
        }
    }

    private static Map<String, SynchronizationStrategy> getSynchronizationStrategyMap() {
        Map<String, SynchronizationStrategy> map = new HashMap<>();
        for (SynchronizationStrategy synchronizationStrategy : HaJdbcProperties.SYNCHRONIZATION_STRATEGIES) {
            map.put(synchronizationStrategy.getId(), synchronizationStrategy);
        }
        return map;
    }
}
