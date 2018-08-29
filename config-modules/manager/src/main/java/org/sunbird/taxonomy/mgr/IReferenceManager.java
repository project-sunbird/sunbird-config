package org.sunbird.taxonomy.mgr;

import java.io.File;

import org.sunbird.common.dto.Response;

public interface IReferenceManager {

	Response uploadReferenceDocument(File uploadedFile, String referenceId);

}
