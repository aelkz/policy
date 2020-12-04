package com.redhat.api.policy.exception;

public class ProxyException extends Exception{

	private static String PROXY_EXCEPTION_MESSAGE = "PROXY PROCESSING ERROR";

	public ProxyException(){
		super(PROXY_EXCEPTION_MESSAGE);
	}
	
}
