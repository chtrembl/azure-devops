package com.chtrembl.adoboardsview.model;

import java.io.Serializable;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Component
//singleton calls being made to ADO for this App
@Scope(value = "singleton", proxyMode = ScopedProxyMode.TARGET_CLASS)
@SuppressWarnings("serial")
public class WebRequest implements Serializable {
	private MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

	public MultiValueMap<String, String> getHeaders() {
		return this.headers;
	}

	public void addHeader(String headerKey, String headerValue) {
		this.headers.add(headerKey, headerValue);
	}
}
