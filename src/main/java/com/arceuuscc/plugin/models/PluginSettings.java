package com.arceuuscc.plugin.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginSettings
{
	@Builder.Default
	private int eventPollingInterval = 30;

	@Builder.Default
	private boolean requireClanMembership = true;

	@Builder.Default
	private String clanName = "Arceuus";

	@Builder.Default
	private boolean showNewsletterNotifications = true;
}
