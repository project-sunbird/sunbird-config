package org.sunbird.interceptor;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.dto.ExecutionContext;
import org.sunbird.common.dto.HeaderParam;
import org.sunbird.common.util.RequestWrapper;
import org.sunbird.common.util.ResponseWrapper;
import org.sunbird.telemetry.TelemetryGenerator;
import org.sunbird.telemetry.TelemetryParams;
import org.sunbird.telemetry.logger.TelemetryManager;
import org.sunbird.common.util.AccessEventGenerator;
;

public class ResponseFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		TelemetryGenerator.setComponent("config-service");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		String requestId = getUUID();
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		ExecutionContext.setRequestId(requestId);
		boolean isMultipart = (httpRequest.getHeader("content-type") != null
				&& httpRequest.getHeader("content-type").indexOf("multipart/form-data") != -1);
		String consumerId = httpRequest.getHeader("X-Consumer-ID");
		String channelId = httpRequest.getHeader("X-Channel-Id");
		String appId = httpRequest.getHeader("X-App-Id");
		String path = httpRequest.getRequestURI();
		if (StringUtils.isNotBlank(consumerId))
			ExecutionContext.getCurrent().getGlobalContext().put(HeaderParam.CONSUMER_ID.name(), consumerId);

		if (StringUtils.isNotBlank(channelId))
			ExecutionContext.getCurrent().getGlobalContext().put(HeaderParam.CHANNEL_ID.name(), channelId);
		else
			ExecutionContext.getCurrent().getGlobalContext().put(HeaderParam.CHANNEL_ID.name(), "in.ekstep");

		if (StringUtils.isNotBlank(appId))
			ExecutionContext.getCurrent().getGlobalContext().put(HeaderParam.APP_ID.name(), appId);

		if (!isMultipart && !path.contains("/health")) {
			RequestWrapper requestWrapper = new RequestWrapper(httpRequest);
			TelemetryManager.log("Path: " + requestWrapper.getServletPath() + " | Remote Address: "
					+ request.getRemoteAddr() + " | Params: " + request.getParameterMap());

			ResponseWrapper responseWrapper = new ResponseWrapper((HttpServletResponse) response);
			requestWrapper.setAttribute("startTime", System.currentTimeMillis());
			String env = getEnv(requestWrapper);
			ExecutionContext.getCurrent().getGlobalContext().put(TelemetryParams.ENV.name(), env);
			requestWrapper.setAttribute("env", env);
			chain.doFilter(requestWrapper, responseWrapper);

			AccessEventGenerator.writeTelemetryEventLog(requestWrapper, responseWrapper);
			response.getOutputStream().write(responseWrapper.getData());
		} else {
			TelemetryManager.log("Path: " + httpRequest.getServletPath() + " | Remote Address: "
					+ request.getRemoteAddr() + " | Params: " + request.getParameterMap());
			chain.doFilter(request, response);
		}
	}

	private String getEnv(RequestWrapper requestWrapper) {
		return "config";
	}

	@Override
	public void destroy() {

	}

	private String getUUID() {
		UUID uid = UUID.randomUUID();
		return uid.toString();
	}
}
