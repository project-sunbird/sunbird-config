package org.sunbird.taxonomy.mgr.impl;
import java.io.File;

import org.sunbird.common.Slug;
import org.sunbird.common.dto.Request;
import org.sunbird.common.dto.Response;
import org.sunbird.common.enums.TaxonomyErrorCodes;
import org.sunbird.common.exception.ClientException;
import org.sunbird.common.exception.ServerException;
import org.sunbird.common.mgr.BaseManager;
import org.sunbird.common.util.AWSUploader;
import org.sunbird.common.util.S3PropertyReader;
import org.sunbird.graph.dac.enums.GraphDACParams;
import org.sunbird.graph.dac.model.Node;
import org.sunbird.graph.engine.router.GraphEngineManagers;
import org.sunbird.learning.common.enums.ContentAPIParams;
import org.sunbird.taxonomy.mgr.IReferenceManager;
import org.springframework.stereotype.Component;

@Component
public class ReferenceManagerImpl extends BaseManager implements IReferenceManager {

    
    
    private static final String s3Content = "s3.content.folder";
    private static final String s3Artifacts = "s3.artifact.folder";

    private static final String V2_GRAPH_ID = "domain";
    
	@Override
	public Response uploadReferenceDocument(File uploadedFile, String referenceId) {
        if (null == uploadedFile) {
            throw new ClientException(TaxonomyErrorCodes.ERR_INVALID_UPLOAD_FILE.name(), "Upload file is blank.");
        }
        String[] urlArray = new String[] {};
        try {
        	String folder = S3PropertyReader.getProperty(s3Content) + "/"
					+ Slug.makeSlug(referenceId, true) + "/" + S3PropertyReader.getProperty(s3Artifacts);
            urlArray = AWSUploader.uploadFile(folder, uploadedFile);
        } catch (Exception e) {
            throw new ServerException(TaxonomyErrorCodes.ERR_MEDIA_UPLOAD_FILE.name(),
                    "Error wihile uploading the File.", e);
        }
        String url = urlArray[1];
        
        Request getReferenceRequest = getRequest(V2_GRAPH_ID, GraphEngineManagers.SEARCH_MANAGER, "getDataNode");
        getReferenceRequest.put(GraphDACParams.node_id.name(), referenceId);
		Response res = getResponse(getReferenceRequest);
		
		if(checkError(res)){
			return res;
		}
		
		Node referenceNode = (Node) res.get(GraphDACParams.node.name());
		referenceNode.getMetadata().put(ContentAPIParams.downloadUrl.name(), url);
		
		Request createReq = getRequest(V2_GRAPH_ID, GraphEngineManagers.NODE_MANAGER, "updateDataNode");
		createReq.put(GraphDACParams.node.name(), referenceNode);
		createReq.put(GraphDACParams.node_id.name(), referenceId);
		Response createRes = getResponse(createReq);
		
		if(checkError(createRes)){
			return createRes;
		}
		
		Response response = OK(ContentAPIParams.url.name(), url);
        return response;
	}
}