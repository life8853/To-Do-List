# To-Do List (Android)

A modern Android task management application built with **Kotlin** and **Jetpack Compose** (Material 3), combining clean MVVM/repository architecture with location-aware geofencing reminders, scheduled notifications, and productivity analytics.

---

## Features

- **Task & Category Management** — Create, organize, filter, and track tasks with custom categories, priority levels, and due dates (`TaskList`, `TaskAdder`, `Categories`).
- **Location-Based Reminders & Geofencing** — Attach locations to tasks using an interactive **Google Maps** picker (`LocationPicker`) or by pasting Google Maps links (including automatic resolution of shortened links in `LocationUtils`), backed by `GeofenceManager` and `GeofenceService`.
- **Scheduled Deadline Notifications** — Automated background task alerts managed via `NotificationScheduler`, `NotificationService`, and `TaskNotificationReciever`.
- **Productivity Statistics** — Dedicated analytics view (`StatsScreen`, `TaskStatistics`, `StatsViewModel`) tracking completion rates and task distribution.
- **Offline-First Persistence** — Local relational storage powered by **AndroidX Room (SQLite)** (`Database`, `TaskRepository`, `ITaskRepository`) alongside user preferences in **Jetpack DataStore** (`SettingsManager`).

---

## Tech Stack

- **Language:** Kotlin (JVM Target 11)
- **UI Framework:** Jetpack Compose (Material 3, Navigation Compose)
- **Architecture:** MVVM (`ViewModel` + Repository pattern)
- **Local Storage:** Room Database (with KSP) & Jetpack DataStore Preferences
- **Location & Maps:** Google Play Services Location (`play-services-location`) & Maps Compose (`maps-compose`)
- **Serialization:** Kotlinx Serialization JSON

---

## Getting Started

### Prerequisites

- **Android Studio** (Ladybug or newer recommended)
- **Android SDK:** `minSdk 26` (Android 8.0+) / `compileSdk 36`
- **JDK:** Java 11+

### Build & Run

1. Clone the repository:
   ```bash
   git clone https://github.com/life8853/To-Do-List.git
   cd To-Do-List
   ```
2. Open the project in **Android Studio** and allow Gradle sync to complete, or build from the terminal:
   ```bash
   ./gradlew assembleDebug
   ```
3. Run the `app` configuration on an emulator or physical Android device with Location/Notification permissions enabled.

---

## Contributors

- **Jakub Ochal** ([@life8853](https://github.com/life8853))
- **Akhil KK** ([@akhil-kk15](https://github.com/akhil-kk15))
