package br.gov.bnb.openbanking.policy.ipratelimit.exception;

public class RateLimitException extends Exception{
	
	/**
	 *
	 */
	private static final long serialVersionUID = -4028628438504954208L;

	public RateLimitException() {
		super();
	}
	
	public RateLimitException(String message){
		super(message);
	}
	
}
