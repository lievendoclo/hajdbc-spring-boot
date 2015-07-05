package be.insaneprogramming.springboot.hajdbc;

import net.sf.hajdbc.SimpleDatabaseClusterConfigurationFactory;
import net.sf.hajdbc.SynchronizationStrategy;
import net.sf.hajdbc.balancer.BalancerFactory;
import net.sf.hajdbc.balancer.random.RandomBalancerFactory;
import net.sf.hajdbc.balancer.roundrobin.RoundRobinBalancerFactory;
import net.sf.hajdbc.balancer.simple.SimpleBalancerFactory;
import net.sf.hajdbc.cache.DatabaseMetaDataCacheFactory;
import net.sf.hajdbc.cache.eager.EagerDatabaseMetaDataCacheFactory;
import net.sf.hajdbc.cache.eager.SharedEagerDatabaseMetaDataCacheFactory;
import net.sf.hajdbc.cache.lazy.LazyDatabaseMetaDataCacheFactory;
import net.sf.hajdbc.cache.lazy.SharedLazyDatabaseMetaDataCacheFactory;
import net.sf.hajdbc.cache.simple.SimpleDatabaseMetaDataCacheFactory;
import net.sf.hajdbc.sql.Driver;
import net.sf.hajdbc.sql.DriverDatabase;
import net.sf.hajdbc.sql.DriverDatabaseClusterConfiguration;
import net.sf.hajdbc.state.bdb.BerkeleyDBStateManagerFactory;
import net.sf.hajdbc.state.simple.SimpleStateManagerFactory;
import net.sf.hajdbc.state.sql.SQLStateManagerFactory;
import net.sf.hajdbc.state.sqlite.SQLiteStateManagerFactory;
import net.sf.hajdbc.sync.DifferentialSynchronizationStrategy;
import net.sf.hajdbc.sync.DumpRestoreSynchronizationStrategy;
import net.sf.hajdbc.sync.FastDifferentialSynchronizationStrategy;
import net.sf.hajdbc.sync.FullSynchronizationStrategy;
import net.sf.hajdbc.sync.PassiveSynchronizationStrategy;
import net.sf.hajdbc.sync.PerTableSynchronizationStrategy;
import net.sf.hajdbc.util.concurrent.cron.CronExpression;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.text.ParseException;
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
     * Which balancer factory should be used for queries
     */
    String balancerFactory = "round-robin";
    /**
     * What synchronization factory should be used
     */
    String defaultSynchronizationStrategy = "full";
    /**
     * Indicates how the meta data for HA JDBC should be cached
     */
    String databaseMetaDataCacheFactory = "shared-eager";
    /**
     * How the state of the cluster should be managed
     */
    String stateManagerFactory = "simple";
    /**
     * The JDBC URL for an SQL state manager
     */
    String stateManagerUrlPattern;
    /**
     * The user name for am SQL state manager
     */
    String stateManagerUser;
    /**
     * The user password for an SQL state manager
     */
    String stateManagerPassword;
    /**
     * The location pattern for an SqLite or BerkeleyDB state manager
     */
    String stateManagerLocationPattern;
    /**
     * Whether identity column detection should be enabled
     */
    boolean identityColumnDetectionEnabled = true;
    /**
     * Whether sequence detection should be enabled
     */
    boolean sequenceDetectionEnabled = true;

    @PostConstruct
    void register() throws ParseException {
        if (enabled) {
            DriverDatabaseClusterConfiguration config = new DriverDatabaseClusterConfiguration();
            if (driverDatabases != null && driverDatabases.size() > 0) {
                config.setDatabases(driverDatabases);
            } else {
                throw new IllegalStateException("HA JDBC driver databases should be configured to contain at least one driver database");
            }
            config.setDatabaseMetaDataCacheFactory(DatabaseMetaDataCacheChoice.fromId(databaseMetaDataCacheFactory));
            config.setBalancerFactory(BalancerChoice.fromId(balancerFactory));
            switch (stateManagerFactory) {
                case "sql":
                    SQLStateManagerFactory sqlStateManagerFactory = new SQLStateManagerFactory();
                    if (stateManagerUrlPattern != null)
                        sqlStateManagerFactory.setUrlPattern(stateManagerUrlPattern);
                    if (stateManagerUser != null)
                        sqlStateManagerFactory.setUser(stateManagerUser);
                    if (stateManagerPassword != null)
                        sqlStateManagerFactory.setPassword(stateManagerPassword);
                    config.setStateManagerFactory(sqlStateManagerFactory);
                    break;

                case "berkeleydb":
                    BerkeleyDBStateManagerFactory
                        berkeleyDBStateManagerFactory = new BerkeleyDBStateManagerFactory();
                    if (stateManagerLocationPattern != null)
                        berkeleyDBStateManagerFactory.setLocationPattern(stateManagerLocationPattern);
                    config.setStateManagerFactory(berkeleyDBStateManagerFactory);
                    break;
                case "sqlite":
                    SQLiteStateManagerFactory sqLiteStateManagerFactory = new SQLiteStateManagerFactory();
                    if (stateManagerLocationPattern != null)
                        sqLiteStateManagerFactory.setLocationPattern(stateManagerLocationPattern);
                    config.setStateManagerFactory(sqLiteStateManagerFactory);
                    break;
                default:
                    config.setStateManagerFactory(new SimpleStateManagerFactory());
            }
            config.setSynchronizationStrategyMap(SynchronizationStrategyChoice.getIdMap());
            config.setDefaultSynchronizationStrategy(defaultSynchronizationStrategy);
            config.setIdentityColumnDetectionEnabled(identityColumnDetectionEnabled);
            config.setSequenceDetectionEnabled(sequenceDetectionEnabled);
            config.setAutoActivationExpression(new CronExpression(cronExpression));

            Driver.setConfigurationFactory(clusterName,
                                           new SimpleDatabaseClusterConfigurationFactory<java.sql.Driver, DriverDatabase>(config));
        }
    }

    private enum DatabaseMetaDataCacheChoice {
        SIMPLE(new SimpleDatabaseMetaDataCacheFactory()),
        LAZY(new LazyDatabaseMetaDataCacheFactory()),
        EAGER(new EagerDatabaseMetaDataCacheFactory()),
        SHARED_LAZY(new SharedLazyDatabaseMetaDataCacheFactory()),
        SHARED_EAGER(new SharedEagerDatabaseMetaDataCacheFactory());

        DatabaseMetaDataCacheFactory databaseMetaDataCacheFactory;

        DatabaseMetaDataCacheChoice(DatabaseMetaDataCacheFactory databaseMetaDataCacheFactory) {
            this.databaseMetaDataCacheFactory = databaseMetaDataCacheFactory;
        }

        static DatabaseMetaDataCacheFactory fromId(String id) {
            for (DatabaseMetaDataCacheChoice databaseMetaDataCacheChoice : DatabaseMetaDataCacheChoice.values()) {
                if(databaseMetaDataCacheChoice.databaseMetaDataCacheFactory.getId().equals(id)) {
                    return databaseMetaDataCacheChoice.databaseMetaDataCacheFactory;
                }
            }
            throw new IllegalArgumentException("Could not find database metadata cache factory with id " + id);
        }
    }

    private enum BalancerChoice {
        ROUND_ROBIN(new RoundRobinBalancerFactory()),
        RANDOM(new RandomBalancerFactory()),
        SIMPLE(new SimpleBalancerFactory());

        BalancerFactory balancerFactory;

        BalancerChoice(BalancerFactory balancerFactory) {
            this.balancerFactory = balancerFactory;
        }

        static BalancerFactory fromId(String id) {
            for (BalancerChoice balancerChoice : BalancerChoice.values()) {
                if(balancerChoice.balancerFactory.getId().equals(id)) {
                    return balancerChoice.balancerFactory;
                }
            }
            throw new IllegalArgumentException("Could not find balancer factory with id " + id);
        }
    }

    private enum SynchronizationStrategyChoice {
        FULL(new FullSynchronizationStrategy()),
        DUMP_RESTORE(new DumpRestoreSynchronizationStrategy()),
        DIFF(new DifferentialSynchronizationStrategy()),
        FASTDIFF(new FastDifferentialSynchronizationStrategy()),
        PER_TABLE_FULL(new PerTableSynchronizationStrategy(new FullSynchronizationStrategy())),
        PER_TABLE_DIFF(new PerTableSynchronizationStrategy(new DifferentialSynchronizationStrategy())),
        PASSIVE(new PassiveSynchronizationStrategy());

        SynchronizationStrategy synchronizationStrategy;

        SynchronizationStrategyChoice(SynchronizationStrategy synchronizationStrategy) {
            this.synchronizationStrategy = synchronizationStrategy;
        }

        static SynchronizationStrategy fromId(String id) {
            for (SynchronizationStrategyChoice synchronizationStrategyChoice : SynchronizationStrategyChoice.values()) {
                if(synchronizationStrategyChoice.synchronizationStrategy.getId().equals(id)) {
                    return synchronizationStrategyChoice.synchronizationStrategy;
                }
            }
            throw new IllegalArgumentException("Could not find synchronization strategy with id " + id);
        }

        static Map<String, SynchronizationStrategy> getIdMap() {
            Map<String, SynchronizationStrategy> strategies = new HashMap<>();
            for (SynchronizationStrategyChoice synchronizationStrategyChoice : SynchronizationStrategyChoice.values()) {
                strategies.put(synchronizationStrategyChoice.synchronizationStrategy.getId(),
                               synchronizationStrategyChoice.synchronizationStrategy);
            }
            return strategies;
        }
    }
}
