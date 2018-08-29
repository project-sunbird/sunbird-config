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

        if(StringUtils.equalsIgnoreCase(cloudStoreType, "azure")) {
            String storageKey = Platform.config.getString("azure_storage_key");
            String storageSecret = Platform.config.getString("azure_storage_secret");
            storageService = StorageServiceFactory.getStorageService(new StorageConfig(cloudStoreType, storageKey, storageSecret));
        }else if(StringUtils.equalsIgnoreCase(cloudStoreType, "aws")) {
            String storageKey = Platform.config.getString("aws_storage_key");
            String storageSecret = Platform.config.getString("aws_storage_secret");
            storageService = StorageServiceFactory.getStorageService(new StorageConfig(cloudStoreType, storageKey, storageSecret));
        }else {
            throw new ServerException("ERR_INVALID_CLOUD_STORAGE", "Error while initialising cloud storage");
        }


//        try{
//            TelemetryManager.error("in cloud store");
//            String storageKey = "bajaj";
//            String storageSecret = "n9BdvADbFsoKHlGDtBbwROtJ5bDraxRo0lp/39iZqPbERuPpzzWuMlV+k8qMWaGO9D9LcB4aSjJWmtkO6mWgDA==";
//            storageService = StorageServiceFactory.getStorageService(new StorageConfig(cloudStoreType, storageKey, storageSecret));
//            TelemetryManager.error("in cloud store after initialization");
//        } catch(Exception e){
//            TelemetryManager.error("Get file size from Azure | Exception: " + e.getMessage(), e);
//        }


    }

//    public static BaseStorageService getCloudStoreService() {
//        return storageService;
//    }

    public static String getContainerName() {
        if(StringUtils.equalsIgnoreCase(cloudStoreType, "azure")) {
            return Platform.config.getString("azure_storage_container");
        }else if(StringUtils.equalsIgnoreCase(cloudStoreType, "aws")) {
            return S3PropertyReader.getProperty("aws_storage_container");
        }else {
            throw new ServerException("ERR_INVALID_CLOUD_STORAGE", "Error while getting container name");
        }
    }

//    public static String[] uploadFile(String folderName, File file, boolean slugFile) throws Exception {
//        if (BooleanUtils.isTrue(slugFile))
//            file = Slug.createSlugFile(file);
//        String objectKey = folderName + "/" + file.getName();
//        String container = getContainerName();
//        String url = storageService.upload(container, file.getAbsolutePath(), objectKey, Option.apply(false), Option.apply(false), Option.empty(), Option.empty());
//        return new String[] { objectKey, url};
//    }


    public static Blob getObject(String key) throws Exception {
        System.out.println("In get object size");
        String container = getContainerName();
        System.out.println("Container: " + container);
        Blob blob = null;
        blob = (Blob) storageService.getObject(container, key, Option.apply(false));
        return blob;
    }

    public static String getObjectData(String key) throws Exception {
        System.out.println("In get object size");
        String container = getContainerName();
        System.out.println("Container: " + container);
        String data[];
        data = storageService.getObjectData(container, key);

        String str = StringUtils.join(data);
        System.out.println("Joined Data: " + str);
        String response = StringEscapeUtils.unescapeJava(str);
        System.out.println("Unescaped Data: " + response);
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

//    public static void copyObjectsByPrefix(String sourcePrefix, String destinationPrefix) {
//        String container = getContainerName();
//        storageService.copyObjects(container, sourcePrefix, container, destinationPrefix, Option.apply(true));
//    }
//
//    public static String getURL(String prefix) {
//        String container = getContainerName();
//        Blob blob =  (Blob)storageService.getObject(container, prefix, Option.apply(false));
//        Map<String, Object> map = scala.collection.JavaConversions.mapAsJavaMap(blob.metadata());
//        return (String)map.get("uri");
//    }
//
//
//    public static void deleteFile(String key, boolean isDirectory) throws Exception {
//        String container = getContainerName();
//        storageService.deleteObject(container, key, Option.apply(isDirectory));
//    }

}