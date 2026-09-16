package com.harmoniedev.api.exception;

/** Thrown when a tenant's action would exceed a limit or a disabled feature of their current subscription plan. */
public class PlanLimitExceededException extends RuntimeException {
	public PlanLimitExceededException(String message) {
		super(message);
	}
}
