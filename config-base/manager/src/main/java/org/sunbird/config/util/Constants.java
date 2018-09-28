package org.sunbird.config.util;

public class Constants {
    public static String CASSANDRA_KEYSPACE = "sunbird";
    public static String CASSANDRA_AUDIT_TABLE = "config_path_audit";
    public static String CASSANDRA_AUDIT_COLUMN_PATH = "cloud_store_path";
    public static String CASSANDRA_AUDIT_COLUMN_DATE = "created_date";
    public static String CASSANDRA_AUDIT_COLUMN_KEY = "id";
    public static String CASSANDRA_AUDIT_COLUMN_VERSION = "version";
    public static String CASSANDRA_AUDIT_COLUMN_CS_TYPE = "cloud_store_type";
    public static String CASSANDRA_AUDIT_COLUMN_CS_ACCOUNT = "cloud_store_account";
    public static String CASSANDRA_AUDIT_COLUMN_CS_CONTAINER = "cloud_store_container";

    public static String CONFIG_STORAGE_KEY = "config";
    public static String CONFIG_ROOT_KEY = "_instance";
    public static String CONFIG_TENANT_IDENTIFIER = "_tenant";
    public static String CONFIG_ORGANISATION_IDENTIFIER = "_organisation";
}
