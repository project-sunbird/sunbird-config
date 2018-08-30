package org.sunbird.config.util;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
//import Platform;
import org.sunbird.common.Platform;
import org.sunbird.common.exception.ServerException;
import org.sunbird.common.util.S3PropertyReader;
import org.sunbird.telemetry.logger.TelemetryManager;
import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.cloud.storage.Model.Blob;
//import org.sunbird.cloud.storage.conf.AppConf;
import org.sunbird.cloud.storage.factory.StorageConfig;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;

import scala.Option;
//import scala.collection.JavaConversions;

public class CloudStore {

    private static BaseStorageService storageService = null;
    private static String cloudStoreType = Platform.config.getString("cloud_storage_type");

    static {
        TelemetryManager.error("Cloud storage type 1" + Platform.config.getString("cloud_storage_type"));
        TelemetryManager.error("Azure storage key 1" + Platform.config.getString("azure_storage_key"));

        if(StringUtils.equalsIgnoreCase(cloudStoreType, "azure")) {
            String storageKey = Platform.config.getString("azure_storage_key");
            String storageSecret = Platform.config.getString("azure_storage_secret");
            storageService = StorageServiceFactory.getStorageService(new StorageConfig(cloudStoreType, storageKey, storageSecret));
        }else if(StringUtils.equalsIgnoreCase(cloudStoreType, "aws")) {
            String storageKey = Platform.config.getString("aws_storage_key");
            String storageSecret = Platform.config.getString("aws_storage_secret");
            storageService = StorageServiceFactory.getStorageService(new StorageConfig(cloudStoreType, storageKey, storageSecret));
        }else {
            TelemetryManager.error("Cloud storage type 2" + Platform.config.getString("cloud_storage_type"));
            TelemetryManager.error("Azure storage key 2" + Platform.config.getString("azure_storage_key"));
            throw new ServerException("ERR_INVALID_CLOUD_STORAGE", "Error while initialising cloud storage");
        }
    }

    public static String getContainerName() {
        if(StringUtils.equalsIgnoreCase(cloudStoreType, "azure")) {
            return Platform.config.getString("azure_storage_container");
        }else if(StringUtils.equalsIgnoreCase(cloudStoreType, "aws")) {
            return S3PropertyReader.getProperty("aws_storage_container");
        }else {
            throw new ServerException("ERR_INVALID_CLOUD_STORAGE", "Error while getting container name");
        }
    }

    public static Blob getObject(String key) throws Exception {
        String container = getContainerName();
        Blob blob = null;
        blob = (Blob) storageService.getObject(container, key, Option.apply(false));
        return blob;
    }

    public static String getObjectData(String key) throws Exception {
        String container = getContainerName();
        String data[];
        data = storageService.getObjectData(container, key);

        String str = StringUtils.join(data);
        String response = StringEscapeUtils.unescapeJava(str);
        return response;
    }

    public static double getObjectSize(String key) throws Exception {
        Blob blob = null;
        blob = (Blob) getObject(key);
        return blob.contentLength();
    }

    public static String getObjectString(String key) throws Exception {
        Blob blob = null;
        blob = (Blob) getObject(key);
        String blobString = blob.toString();
        return blobString;
    }
}