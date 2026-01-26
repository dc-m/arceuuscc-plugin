package com.arceuuscc.plugin.models;

import lombok.Data;
import java.util.List;

@Data
public class Event
{
	private String eventId;
	private String title;
	private String startTime;
	private int durationMinutes;
	private String description;
	private String status;
	private String codeword;
	private List<Signup> signups;
}
