package com.arceuuscc.plugin.models;

/**
 * Authorization state for the current user.
 */
public enum AuthorizationState
{
	UNKNOWN,
	NO_TOKEN,
	PENDING,
	ACCEPTED,
	REJECTED,
	REVOKED;

	public static AuthorizationState fromApiStatus(String status)
	{
		if (status == null)
		{
			return UNKNOWN;
		}
		try
		{
			return valueOf(status.toUpperCase());
		}
		catch (IllegalArgumentException e)
		{
			return UNKNOWN;
		}
	}

	public boolean hasAccess()
	{
		return this == ACCEPTED;
	}

	public boolean isPending()
	{
		return this == PENDING || this == NO_TOKEN || this == UNKNOWN;
	}
}
