package com.shareplate.model;

/**
 * Abstraction defining role-based behaviors and polymorphism across different user types.
 */
public interface RoleAccessible {
    /**
     * Returns the appropriate dashboard page URL for the user's role.
     */
    String getDashboardUrl();

    /**
     * Determines whether this user type is authorized to create/post food donations.
     */
    boolean canCreateDonation();

    /**
     * Determines whether this user type is authorized to request food donations.
     */
    boolean canRequestDonation();

    /**
     * Human-readable display title for this user's role.
     */
    String getRoleDisplayName();
}
