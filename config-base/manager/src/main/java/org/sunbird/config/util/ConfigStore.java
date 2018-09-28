package org.sunbird.config.util;

import akka.util.Switch;
import com.datastax.driver.core.Row;
import com.sun.org.apache.xpath.internal.operations.Bool;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;
import org.sunbird.cassandra.store.CassandraStoreImpl;
import org.sunbird.common.Platform;
import org.sunbird.common.exception.ServerException;

import java.time.Instant;
import java.util.*;

public class ConfigStore {

    private static Map<String, Object> configStore = new HashMap<>();
    private static CassandraStoreImpl auditStore = new CassandraStoreImpl();
    private static String cloudStoreType = Platform.config.getString("cloud_storage_type");

    static {
        auditStore.initialise(Constants.CASSANDRA_KEYSPACE, Constants.CASSANDRA_AUDIT_TABLE, null, true);
    }

    //TODO Refactor in-memory storage interaction

    /**
     * Returns the value of the provided key from the Hashmap
     *
     * @param key Key of the configuration to be retrieved
     * @return Object
     */
    private static Object getConfig(String key) {
        return configStore.get(key);
    }

    /**
     * Stores the provided configuration data (as key, value) in the Hashmap
     *
     * @param key   Key of the configuration
     * @param value Value of the configuration
     */
    private static void setConfig(String key, Object value) {
        configStore.put(key, value);
    }

    /**
     * Returns the value of the provided key from the Hashmap
     *
     * @param key Key of the configuration to be retrieved
     * @return Boolean
     */
    public static Boolean isConfigKeyExists(String key) {
        return configStore.containsKey(key);
    }

    /**
     * Reads the storage and gets the count of the config keys
     * @return Integer
     */
    public static Integer getConfigCount() {
        Set<Map.Entry<String, Object>> configKeys = configStore.entrySet();
        return configKeys.size();
    }

    /**
     * Get the timestamp when the configurations were refreshed last
     * @return Long
     */
    public static Long getLastRefreshTimestamp() {
        Long timestamp = 0L;
        Row latestRecord = auditStore.getLatestRecordTimestamp(Constants.CASSANDRA_AUDIT_COLUMN_DATE, Constants.CASSANDRA_AUDIT_COLUMN_CS_TYPE, cloudStoreType);

        if (!latestRecord.isNull(Constants.CASSANDRA_AUDIT_COLUMN_DATE)) {
            timestamp = latestRecord.getTime(Constants.CASSANDRA_AUDIT_COLUMN_DATE);
        }
        return timestamp;
    }

    /**
     * Clears the current config data
     */
    private static void clearConfig() {
        configStore.clear();
    }

    public static Boolean refresh(String configPath) {
        try {
            //Get the config data from cloud store
            String configData = CloudStore.getObjectData(configPath);

            //Parse the received config using typesafe config utility
            Config parsedConfigData = ConfigFactory.parseString(configData);
            Set parsedConfigDataList = parsedConfigData.entrySet();

            //If there is some data provided
            if (parsedConfigDataList.size() > 0) {
                //Save the path as Audit log to be used on service restart
                Long currentEpoch = Instant.now().toEpochMilli();
                String id = UUID.randomUUID().toString();

                Map<String, Object> cloudStoreConfig = CloudStore.getConfig();
                cloudStoreConfig.put(Constants.CASSANDRA_AUDIT_COLUMN_PATH, configPath);
                cloudStoreConfig.put(Constants.CASSANDRA_AUDIT_COLUMN_DATE, currentEpoch);
                cloudStoreConfig.put(Constants.CASSANDRA_AUDIT_COLUMN_VERSION, currentEpoch);
                cloudStoreConfig.put(Constants.CASSANDRA_AUDIT_COLUMN_KEY, id);
                auditStore.insert(id, cloudStoreConfig);

                //Clear the previous data
                clearConfig();

                // Iterate over flat config and Store
//                for (Map.Entry<String, ConfigValue> entry : parsedConfigData.entrySet()) {
//                    String key = entry.getKey();
//                    Object val = entry.getValue().unwrapped();
//                    setConfig(key, val);
//                }
                setConfig(Constants.CONFIG_STORAGE_KEY, parsedConfigData);
            }
        } catch (Exception e) {
            throw new ServerException("ERR_REFRESH_CONFIG_DATA", "Error while refreshing config data.");
        }
        return true;
    }

    public static Object read(String configKeyWithScope) {
        //Split the config key into scope and key
        String[] configParts = configKeyWithScope.split("\\.");
        Integer configScopeLength = configParts.length - 1;

        if ((!Objects.equals(configParts[0], (Constants.CONFIG_ROOT_KEY).substring(1))) || (configScopeLength > 3)) {
            //TODO return error
        }

        Config configurations = (Config) getConfig(Constants.CONFIG_STORAGE_KEY);
        String configKey = (configParts[configParts.length - 1]).replace("/", ".");
        Object responseConfig = null;

        switch (configScopeLength) {
            case 0:
                responseConfig = configurations.getAnyRef(Constants.CONFIG_ROOT_KEY);
                break;
            case 1:
                responseConfig = configurations.getAnyRef(Constants.CONFIG_ROOT_KEY + "." + configKey);
                break;
            case 2:
                String tenantConfig = configurations.getAnyRef(Constants.CONFIG_ROOT_KEY + "." + Constants.CONFIG_TENANT_IDENTIFIER + "." + configParts[1]).toString();
                Config tenantInstanceConfig = configurations.getConfig(Constants.CONFIG_ROOT_KEY);
                Config tenantFallbackConfig = ConfigFactory.parseString(tenantConfig).withFallback(tenantInstanceConfig);
                responseConfig = tenantFallbackConfig.getAnyRef(configKey);
                break;
            case 3:
                String orgConfig = configurations.getAnyRef(Constants.CONFIG_ROOT_KEY + "." + Constants.CONFIG_TENANT_IDENTIFIER + "." + configParts[1] + "." + Constants.CONFIG_ORGANISATION_IDENTIFIER + "." + configParts[2]).toString();
                Config orgInstanceConfig = configurations.getConfig(Constants.CONFIG_ROOT_KEY);
                Config orgFallbackConfig = ConfigFactory.parseString(orgConfig).withFallback(orgInstanceConfig);
                responseConfig = orgFallbackConfig.getAnyRef(configKey);
                break;
        }

        return responseConfig;
    }
}
