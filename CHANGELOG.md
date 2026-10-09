# Changelog

All notable changes to the Arceuus CC RuneLite Plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [2.5.0] - 2026-10-09

### Added
- **Codeword Overlay**: The codeword and current UTC date/time for an active event now show on their own one-line overlay
  - It can be moved separately from the event overlay and shows in every overlay mode (Detailed, Minimal and Icon)
  - New "Show Codeword Overlay" setting turns it on or off for all events
  - Each active event with a codeword has a Hide Codeword Overlay / Show Codeword Overlay button in the sidebar
  - The setting and the per-event button are independent of each other, and of Show Overlay / Hide Overlay

### Changed
- The codeword and date/time are no longer shown inside the Detailed and Minimal overlays or the icon tooltip
- Icon text is now always white; it was black (and unreadable) for active events with a codeword

## [2.4.1] - 2026-10-07

### Added
- The sidebar event card now shows the codeword for active events that have one

### Fixed
- **No-Signup Events Missing From Overlay**: Active events that don't use signups now show on the overlay and as an icon (with their codeword), instead of never appearing because nobody could sign up
  - These events get a Hide Overlay / Show Overlay button in the sidebar while active
  - If you marked the event Not Interested before it started, it stays off the overlay until you click Show Overlay

## [2.4.0] - 2026-10-03

### Added
- **New Events On The Overlay**: Unread upcoming events now show on the overlay with a "NEW" label, however far away they are
  - They stay until you read them (View Details, Mark as Read, Mark all as read) or click Not Interested
  - Once read, an event comes back on the overlay when it is within 3 hours of starting, as before
  - Works in all overlay modes; Icon mode shows "NEW" in the icon tooltip
- **Mark as Read / Mark as Unread**: Each upcoming event in the sidebar has a button to flip it between read and unread
  - Marking an event unread puts it back on the overlay as a reminder

### Changed
- Icon mode now follows the same rule as the other modes: an upcoming event only shows while unread or within 3 hours of starting (previously every upcoming event showed an icon)
- The overlay can list several upcoming events at once, sorted soonest first
- With "Show Starting Soon" turned off, the overlay skips events inside the 30 minute window and shows the next one, instead of showing nothing
- Sidebar event buttons are now full width and all the same size

### Fixed
- **Minimal Overlay Overflow**: Long event titles no longer spill outside the overlay box; the box grows to fit and very long titles are shortened
- **Signups On No-Signup Events**: The "Signups" count is no longer shown on the overlay, icon tooltip or details dialog for events that don't use signups

## [2.3.2] - 2026-08-15

### Fixed
- **Countdowns Off By One Hour**: Fixed all countdown timers (overlay, infobox tooltips, sidebar) being an hour short for users on BST/other non-UTC timezones
  - Event times are stored in UTC but were compared against the local clock; countdowns now use UTC consistently
  - Upcoming events now appear on the overlay at the correct time, and "starting soon"/"ending soon" indicators trigger at the right moment
  - "Event passed" detection in the sidebar no longer marks events as passed an hour early

## [2.3.1] - 2026-07-26

### Added
- Events with codewords now show the current UTC date and time on the overlay and icon tooltip

## [2.3.0] - 2026-03-11

### Added
- **Signups Optional Per Event**: Events can now have signups turned off; those events show "No signup required" and have no Sign Up button

## [2.2.0] - 2026-03-08

### Added
- **Multi-Page Newsletters**: Newsletters with more than one page can now be paged through in the sidebar

### Fixed
- Fixed the icon not showing for events you are signed up to

## [2.1.4] - 2026-02-20

### Added
- The overlay now tells you to click the Arceuus CC sidebar icon to request authorization when you're in the clan but not yet authorized
- Icon is now the default overlay mode

### Fixed
- Fixed the sidebar flashing while unauthorized
- Icons and notifications no longer show when you don't have access to the plugin

## [2.1.3] - 2026-02-09

### Fixed
- **Auth Lost After Inactivity Logout**: Fixed users being forced to re-request access after being logged out due to inactivity
  - Auth token now properly reloads when player name becomes available after login, resolving a race condition where the player entity wasn't ready when the LOGGED_IN game state fired
  - Added resilience against transient API failures clearing auth tokens (requires 3 consecutive "not found" responses before clearing)

## [2.1.2] - 2026-02-09

### Fixed
- **UI Flashing**: Fixed sidebar panel constantly flashing/rebuilding during authorization polling
  - Panel now only rebuilds when the display state actually changes
  - Added state tracking to skip unnecessary UI updates
- **False "Clan Required" Overlay**: Fixed overlay incorrectly showing "login to the clan" message when teleporting or loading new map areas
  - Added debouncing to clan membership detection (requires 5 consecutive null readings before marking as not in clan)
  - Prevents false negatives when clan channel data is briefly unavailable during loading

## [2.1.1] - 2026-02-07

### Fixed
- **Auth Token Persistence**: Auth tokens are now stored per-player instead of globally
  - Fixes issue where logging into a different OSRS account would cause the auth token to be cleared
  - Each account now maintains its own authorization status
  - Switching between accounts no longer requires re-requesting access

## [2.1.0] - 2026-01-31

### Added
- **Icon Overlay Mode**: New "Icon" overlay mode using RuneLite's native InfoBox system
  - Displays compact event icons alongside other RuneLite InfoBoxes
  - Abbreviated event title (first 4 characters) shown on each icon
  - Hover tooltip with full event details: title, countdown, signups, and codeword
  - Supports multiple simultaneous events as separate icons
  - Newsletter icon shown when unread newsletters are available
- **Per-Event Overlay Control**: "Show/Hide Overlay" button on event cards when signed up
  - Hide individual events from the in-game overlay without affecting other events
  - Persisted across client restarts
- **Not Interested**: "Not Interested" button on upcoming events you haven't signed up for
  - Hides the event from the overlay
  - Automatically cleared if you sign up for the event
  - Toggle back with "Show Again" button

### Changed
- Active events now only show in the overlay when you are signed up
- Overlay mode options are now: Detailed, Minimal, and Icon

## [2.0.1] - 2026-01-31

### Fixed
- **Leave Event**: Fixed unable to leave/unsign from events
- **Duration Display**: Duration now shows days and hours (e.g. "14d" instead of "336h")
- **Multiple Active Events**: Overlay now shows all active events instead of only the last one

### Added
- **Sidebar Countdown**: Event cards in the sidebar now show a countdown (e.g. "Starts in: 2d 5h" or "Ends in: 1h 30m")

### Changed
- Overlay width now auto-sizes to fit content instead of using fixed widths

## [2.0.0] - 2025-01-26

### Added
- **Authorization System**: Users must now request access to use the plugin
  - Request access with a single click when logged in
  - Access requests are reviewed by clan staff
  - Auth code displayed in header panel (click to copy for verification)
  - Automatic status checking while waiting for approval
- **Requirements Panel**: Clear messaging when not logged in or not in clan
- **Build Environments**: Support for test/prod API environments via Gradle build flags

### Changed
- Content now always refreshes automatically (removed Auto Refresh configuration option)
- Improved polling using scheduled timers instead of threads
- Auth headers sent with all API requests for access control

### Security
- All plugin features now require authorization approval
- Signup/unsignup actions validate authorization status
- Unauthorized users cannot view events or newsletters

## [1.2.0] - 2025-01-26

### Added
- **Event Codewords**: Event organizers can set an optional codeword that is revealed only when the event becomes active (hidden during UPCOMING, visible when ACTIVE)
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
