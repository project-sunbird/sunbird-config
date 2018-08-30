package org.sunbird.util;

import org.sunbird.common.exception.ClientException;
import org.sunbird.graph.common.exception.GraphEngineErrorCodes;


public class AuditLogUtil {

    public static String createObjectId(String graphId, String objectId) {
        if(graphId == null) 
            throw new ClientException(GraphEngineErrorCodes.ERR_INVALID_GRAPH_ID.name(), "GraphID is blank.");
        String auditObjectId = "";
        if(objectId == null) {
            auditObjectId = "g-"+graphId;
        } else {
            auditObjectId = "n-"+graphId+"::"+objectId;
        }
        return auditObjectId;
    }
}
