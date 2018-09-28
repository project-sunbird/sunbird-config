package org.sunbird.config.packageListener;

import com.datastax.driver.core.Row;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.store.CassandraStoreImpl;
import org.sunbird.common.Platform;
import org.sunbird.config.util.ConfigStore;
import org.sunbird.config.util.Constants;
import org.sunbird.telemetry.logger.TelemetryManager;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

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
            log(err);
            TelemetryManager.error(err);
        }
    }

    private String getConfigPath() {
        String configPath = "";
        String id = "";

        Long lastRefreshTimestamp = ConfigStore.getLastRefreshTimestamp();

        Row lastAuditRecord = auditStore.getLatestRecord(Constants.CASSANDRA_AUDIT_COLUMN_DATE, lastRefreshTimestamp);
        if ((lastAuditRecord != null) && (!lastAuditRecord.isNull(Constants.CASSANDRA_AUDIT_COLUMN_PATH))) {
            log("Last refresh record: " + lastAuditRecord.toString());
            configPath = lastAuditRecord.getObject(Constants.CASSANDRA_AUDIT_COLUMN_PATH).toString();
            id = lastAuditRecord.getString(Constants.CASSANDRA_AUDIT_COLUMN_KEY);
            log("ID of the current record: " + id);
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
