package com.redhat.policy.exception;

public class RateLimitException extends Exception{
	
	public RateLimitException(){
		super();
	}
	
	public RateLimitException(String message){
		super(message);
	}
	
}
