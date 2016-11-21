package be.insaneprogramming.springboot.hajdbc
import org.springframework.beans.factory.BeanCreationException
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.util.EnvironmentTestUtils
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.FailsWith
import spock.lang.Specification

import javax.sql.DataSource

class HaJdbcAutoConfigurationTests extends Specification {

    AnnotationConfigApplicationContext context

    def setup() {
        context = new AnnotationConfigApplicationContext();
        EnvironmentTestUtils.addEnvironment((ConfigurableApplicationContext) this.context,
                                            "spring.datasource.initialize:false",
                                            "spring.datasource.url:jdbc:hsqldb:mem:testdb-" + new Random().nextInt());
    }

    def cleanup() {
        if (this.context != null) {
            this.context.close();
        }
    }

    @FailsWith(BeanCreationException)
    def "missing HA JDBC driver database configuration should throw exception"() {
        when:
        context.register(DataSourceAutoConfiguration.class,
                         PropertyPlaceholderAutoConfiguration.class,
                         HaJdbcAutoConfiguration);
        this.context.refresh();
        def datasource = this.context.getBean(DataSource)
        then:
        datasource != null

    }

    def "check basic HA JDBC driver database configuration"() {
        when:
        EnvironmentTestUtils.addEnvironment(
                this.context,
                "hajdbc.driverDatabases[0].id=db1",
                "hajdbc.driverDatabases[0].location=jdbc:hsqldb:mem:testdb-" + new Random().nextInt(),
                "hajdbc.driverDatabases[0].user=sa",
                "hajdbc.driverDatabases[1].id=db2",
                "hajdbc.driverDatabases[1].location=jdbc:hsqldb:mem:testdb-" + new Random().nextInt(),
                "hajdbc.driverDatabases[1].user=sa",
                "spring.datasource.url=jdbc:ha-jdbc:default",
                "spring.datasource.username=sa",
                "spring.datasource.driver-class-name=net.sf.hajdbc.sql.Driver"
        );
        context.register(DataSourceAutoConfiguration.class,
                         PropertyPlaceholderAutoConfiguration.class,
                         HaJdbcAutoConfiguration);
        this.context.refresh();
        def datasource = this.context.getBean(DataSource)
        then:
        datasource != null
    }
}