# Changelog

All notable changes to the Arceuus CC RuneLite Plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.2.0] - 2025-01-26

### Added
- **Event Codewords**: Event organizers can set an optional codeword that is revealed only when the event becomes active
  - Hidden during UPCOMING status to prevent early leaks
  - Displayed on both in-game overlay and Discord embed when event is ACTIVE
- **Overlay Mode Setting**: Choose between Detailed and Minimal overlay display modes
  - Detailed: Full multi-line display with all event information
  - Minimal: Compact view with title, status, countdown, and codeword on fewer lines
- **Live Event Signups**: Players can now sign up for events while they are ACTIVE (not just UPCOMING)

### Changed
- Overlay panel widths increased for better readability (180px detailed, 350px minimal)
- Minimal mode uses color-coded display matching detailed view styling

## [1.1.0] - 2025-01-10

### Added
- **Newsletter Support**: View clan newsletters directly within RuneLite with full image rendering
- **Read/Unread Tracking**: Visual mail icons indicate whether events and newsletters have been viewed
  - Closed envelope icon for unread content
  - Open envelope icon for read content
- **Login Notifications**: Receive alerts for any unread events or newsletters when logging in
- **Newsletter Overlay Alert**: In-game overlay now displays when a new newsletter is available
- **Persistent Read State**: Your read/unread status is saved across client restarts
- **Visual Enhancements**: Gold border and highlighting for unseen events to draw attention

### Changed
- Event panels now use mail icons instead of "NEW" text badge for cleaner appearance
- Improved panel layout with icons positioned in top-right corner

## [1.0.1] - 2025-01-08

### Fixed
- Resolved game filter errors that occurred during certain in-game activities
- Removed deprecated crew membership setting that was no longer functional

## [1.0.0] - 2025-01-05

### Added
- **Event Panel**: View all upcoming, active, and completed clan events in the RuneLite sidebar
- **One-Click Signup**: Sign up for events directly from the plugin using your in-game name
- **Real-Time Sync**: Events automatically refresh to show the latest information
- **In-Game Overlay**: See live event countdowns while playing
- **Event Notifications**: Get alerts when events are about to start or new events are posted
- **Color-Coded Status**: Events display different colors based on status (active, upcoming, completed, cancelled)
- **Event Details Dialog**: Click any event to see full description and signup list
- **Clan Verification**: Signup requires membership in the Arceuus CC clan
