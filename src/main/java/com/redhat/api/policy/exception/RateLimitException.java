package com.redhat.api.policy.exception;

public class RateLimitException extends Exception{
	
	private static String RATE_LIMIT_REACHED_MESSAGE = "RATE LIMIT REACHED FOR IP ADDRESS";

	public RateLimitException() {
		super();
	}

	public RateLimitException(String ip){
		super(RATE_LIMIT_REACHED_MESSAGE + " [" + ip + "]");
	}

	public RateLimitException(String message, String ip){
		super(message+"\t"+ip);
	}
	
}
