package be.insaneprogramming.springboot.hajdbc;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import net.sf.hajdbc.SynchronizationStrategy;
import net.sf.hajdbc.sql.DriverDatabase;
import net.sf.hajdbc.sync.*;

/**
 * Configuration properties for HA JDBC integration
 */
@ConfigurationProperties(prefix="hajdbc")
public class HaJdbcProperties {
	static final List<SynchronizationStrategy> SYNCHRONIZATION_STRATEGIES = Arrays.asList(
			new FullSynchronizationStrategy(),
			new DumpRestoreSynchronizationStrategy(),
			new DifferentialSynchronizationStrategy(),
			new FastDifferentialSynchronizationStrategy(),
			new PerTableSynchronizationStrategy(new FullSynchronizationStrategy()),
			new PassiveSynchronizationStrategy()
	);

	/**
	 * Enable or disable HA JDBC integration
	 */
	private boolean enabled = true;
	/**
	 * Configure the backend databases for HA JDBC. Possible properties
	 * are id, location, driver, user and password
	 */
	private List<DriverDatabase> driverDatabases;
	/**
	 * The name of the HA JDBC cluster. To be used in the Spring Boot datasource url
	 */
	private String clusterName = "default";
	/**
	 * The cron expression that indicates when synchronization should occur
	 */
	private String cronExpression = "0 0/1 * 1/1 * ? *";
	/**
	 * What synchronization factory should be used
	 */
	private String defaultSynchronizationStrategy = "full";
	/**
	 * Whether identity column detection should be enabled
	 */
	private boolean identityColumnDetectionEnabled = true;
	/**
	 * Whether sequence detection should be enabled
	 */
	private boolean sequenceDetectionEnabled = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	public List<DriverDatabase> getDriverDatabases() {
		return driverDatabases;
	}

	public void setDriverDatabases(final List<DriverDatabase> driverDatabases) {
		this.driverDatabases = driverDatabases;
	}

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(final String clusterName) {
		this.clusterName = clusterName;
	}

	public String getCronExpression() {
		return cronExpression;
	}

	public void setCronExpression(final String cronExpression) {
		this.cronExpression = cronExpression;
	}

	public String getDefaultSynchronizationStrategy() {
		return defaultSynchronizationStrategy;
	}

	public void setDefaultSynchronizationStrategy(final String defaultSynchronizationStrategy) {
		this.defaultSynchronizationStrategy = defaultSynchronizationStrategy;
	}

	public boolean isIdentityColumnDetectionEnabled() {
		return identityColumnDetectionEnabled;
	}

	public void setIdentityColumnDetectionEnabled(final boolean identityColumnDetectionEnabled) {
		this.identityColumnDetectionEnabled = identityColumnDetectionEnabled;
	}

	public boolean isSequenceDetectionEnabled() {
		return sequenceDetectionEnabled;
	}

	public void setSequenceDetectionEnabled(final boolean sequenceDetectionEnabled) {
		this.sequenceDetectionEnabled = sequenceDetectionEnabled;
	}
}
