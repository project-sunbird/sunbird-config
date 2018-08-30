package org.sunbird.taxonomy.controller;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.sunbird.common.controller.BaseController;
import org.sunbird.common.dto.Response;
import org.sunbird.taxonomy.mgr.IReferenceManager;
import org.sunbird.telemetry.logger.TelemetryManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/v2/reference")
public class ReferenceController extends BaseController {

	

	@Autowired
	private IReferenceManager referenceManager;

	@RequestMapping(value = "/upload/{referenceId:.+}", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Response> uploadReferenceDocument(@PathVariable(value = "referenceId") String referenceId,
			@RequestParam(value = "file", required = true) MultipartFile file) {
		String apiId = "media.upload";
		TelemetryManager.log("Upload | File: " + file);
		try {
			String name = FilenameUtils.getBaseName(file.getOriginalFilename()) + "_" + System.currentTimeMillis() + "."
					+ FilenameUtils.getExtension(file.getOriginalFilename());
			File uploadedFile = new File(name);
			file.transferTo(uploadedFile);
			Response response = referenceManager.uploadReferenceDocument(uploadedFile, referenceId);
			TelemetryManager.log("Upload | Response: " , response.getResult());
			return getResponseEntity(response, apiId, null);
		} catch (Exception e) {
			TelemetryManager.error("Upload | Exception: " + e.getMessage(), e);
			return getExceptionResponseEntity(e, apiId, null);
		}
	}
}
