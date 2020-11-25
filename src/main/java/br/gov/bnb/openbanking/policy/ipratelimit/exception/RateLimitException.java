package br.gov.bnb.openbanking.policy.ipratelimit.exception;

public class RateLimitException extends Exception{
	
	public static String RATE_LIMIT_REACHED_MESSAGE = "RATE LIMIT REACHED FOR IP ";

	private static final long serialVersionUID = -4028628438504954208L;

	public RateLimitException() {
		super();
	}
	
	public RateLimitException(String message){
		super(message);
	}
	
}
