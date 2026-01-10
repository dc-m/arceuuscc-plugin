package com.arceuuscc.plugin.models;

import lombok.Data;

/**
 * Represents a newsletter from the API.
 */
@Data
public class Newsletter {
    private int id;
    private String title;
    private String subtitle;
    private String editorName;
    private String monthYear;
    private String layoutType;
    private String publishedAt;
    private String createdAt;
    private String imageUrl;
    private String contentJson; // Only populated when fetching full newsletter

    /**
     * Check if this newsletter was published after another.
     */
    public boolean isNewerThan(Newsletter other) {
        if (other == null) return true;
        if (this.publishedAt == null) return false;
        if (other.publishedAt == null) return true;
        return this.publishedAt.compareTo(other.publishedAt) > 0;
    }
}
