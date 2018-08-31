package org.sunbird.config.packageListener;

import com.datastax.driver.core.Row;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.store.CassandraStoreImpl;
import org.sunbird.common.Platform;
import org.sunbird.common.exception.ServerException;
import org.sunbird.config.util.ConfigStore;
import org.sunbird.config.util.Constants;
import org.sunbird.telemetry.logger.TelemetryManager;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Iterator;
import java.util.List;

public class RefreshServletContextListener implements ServletContextListener {
    private static CassandraStoreImpl auditStore = new CassandraStoreImpl();

    static {
        auditStore.initialise(Constants.CASSANDRA_KEYSPACE, Constants.CASSANDRA_AUDIT_TABLE, null, true);
    }

    // The servlet context with which we are associated.
    private ServletContext context = null;

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        log("Context destroyed");
        this.context = null;
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        this.context = event.getServletContext();
        String configPath = getConfigPath();
        if (StringUtils.isNotBlank(configPath)) {
            ConfigStore.refresh(configPath);
        } else {
            String err = "Config-Service: Could not load the default configuration on service start. No value to read from database.";
            System.out.println(err);
            TelemetryManager.error(err);
        }
    }

    private String getConfigPath() {
        String configPath = "";
        List<Row> auditRecords = auditStore.read(Constants.CASSANDRA_AUDIT_COLUMN_KEY, Constants.CASSANDRA_CURRENT_ID_VALUE);
        if (auditRecords.size() > 0) {
            Iterator iter = auditRecords.iterator();
            Object auditRecord = iter.next();
            configPath = ((Row) auditRecord).getObject("cloud_store_path").toString();
        }
        return configPath;
    }

    private void log(String message) {
        if (context != null) {
            context.log("MyServletContextListener: " + message);
        } else {
            System.out.println("MyServletContextListener: " + message);
        }
    }
}
