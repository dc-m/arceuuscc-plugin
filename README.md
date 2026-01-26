# Arceuus CC Plugin

A RuneLite plugin for the **Arceuus CC** clan that displays upcoming clan events, newsletters, and allows members to sign up directly from the game.

## Features

- **Event Panel** - View all upcoming, active, and recent clan events in the RuneLite sidebar
- **Newsletter Support** - Read clan newsletters directly in RuneLite
- **In-Game Overlay** - See live event countdowns and newsletter alerts while playing
- **One-Click Signup** - Sign up for events directly from the plugin (even during live events!)
- **Event Codewords** - Secret codewords revealed only when events go live
- **Real-Time Updates** - Events and newsletters sync automatically
- **Notifications** - Get alerts for new events, newsletters, events starting soon, and more
- **Mark as Read** - Track which events and newsletters you've seen
- **Secure Access** - Authorization system ensures only clan members can access content

## How to Use

### Getting Started
1. Log into Old School RuneScape
2. Join the **Arceuus** clan chat
3. Click **Request Access** in the plugin panel
4. Wait for clan staff to approve your request
5. Once approved, you'll have full access to events and newsletters

Your auth code is shown in the header panel - click it to copy if staff need to verify your identity.

### Viewing Events
Click the Arceuus CC icon in the RuneLite sidebar to open the events panel. Events are color-coded:

| Color | Status |
|-------|--------|
| Green | Currently active |
| Gold | New/unseen event |
| Blue | Upcoming |
| Grey | Completed |

Click **View Details** on any event to see full details including description and signup list.

### Newsletters
Switch to the **Newsletters** tab to view clan newsletters. The latest newsletter is highlighted at the top. Click **Read** to view the full newsletter image.

### Signing Up for Events
1. Log into Old School RuneScape
2. Be a member of the Arceuus CC clan
3. Open the plugin panel and click **Sign Up** on any upcoming event
4. Your in-game name will be added to the signup list

You can also withdraw your signup by clicking the button again.

### In-Game Overlay
The overlay shows:
- Active events with time remaining and codeword (if set)
- Upcoming events starting within 3 hours
- New newsletter alerts
- Whether you're signed up for displayed events

Choose between **Detailed** mode (full information) or **Minimal** mode (compact display) in settings.

### Configuration Options

| Setting | Description |
|---------|-------------|
| **Overlay** | |
| Show Overlay | Toggle the in-game event overlay |
| Overlay Mode | Choose Detailed (full info) or Minimal (compact view) |
| Show Active Event | Display currently running events |
| Show Starting Soon | Highlight events starting within 30 minutes |
| Show Upcoming | Display future scheduled events |
| Show Ending Soon | Highlight events ending within 30 minutes |
| Show Newsletter Alert | Display overlay when new newsletter available |
| **Notifications** | |
| Enable Notifications | Master toggle for all notifications |
| Notify Event Starting | Alert when events are about to start |
| Notify New Event | Alert when new events are posted |
| Notify New Newsletter | Alert when new newsletters are published |
| Unread Notifications on Login | Show notifications for unread content when you log in |

## Requirements

- RuneLite client
- Membership in the Arceuus CC clan
- Approved authorization (request access through the plugin)

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for detailed version history.

### Version 2.0.0
- **Authorization System**: Users must request and receive approval to use the plugin
- **Requirements Panel**: Clear messaging when not logged in or not in clan
- Content now always refreshes automatically (removed Auto Refresh setting)
- Improved security with authorization checks on all actions

### Version 1.2.0
- **Event Codewords**: Organizers can set secret codewords revealed only when events become active
- **Overlay Modes**: Choose between Detailed or Minimal overlay display
- **Live Event Signups**: Sign up for events even while they're running
- Improved overlay widths for better readability

### Version 1.1.0
- **Newsletter Support**: Browse and read clan newsletters with full image rendering
- **Read/Unread Tracking**: Mail icons show whether content has been viewed (closed = unread, open = read)
- **Login Notifications**: Get notified of any unread content when you log in
- **Newsletter Overlay**: In-game alert when new newsletters are published
- **Persistent State**: Read status saves across client restarts

### Version 1.0.1
- Fixed game filter errors during certain activities
- Removed deprecated crew membership setting

### Version 1.0.0
- Initial release with event viewing, signup functionality, real-time sync, and notifications

## Support

Join the Arceuus CC Discord server for help and updates.
