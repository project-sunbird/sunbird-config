package org.sunbird.config.util;

public class Constants {
    public static String CASSANDRA_KEYSPACE = "sunbird";
    public static String CASSANDRA_AUDIT_TABLE = "config_path_audit";
    public static String CASSANDRA_AUDIT_COLUMN_PATH = "cloud_store_path";
    public static String CASSANDRA_AUDIT_COLUMN_DATE = "created_date";
    public static String CASSANDRA_AUDIT_COLUMN_KEY = "key";
    public static String CASSANDRA_CURRENT_ID_VALUE = "lastpath";
}
