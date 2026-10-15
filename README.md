# Bookshelf

Bookshelf is a reading list app for Android. Search for books, open one to read more about it, and save the ones you want to keep track of. Saved books stay available without an internet connection, and you can mark each one as read.

Book data comes from [Open Library](https://openlibrary.org/), a free and open book database.

<img width="150" alt="Screenshot_1792069352" src="https://github.com/user-attachments/assets/a52d111b-d670-4e1f-808b-e6ccf5a04c25" />
<img width="150" alt="Screenshot_1792069318" src="https://github.com/user-attachments/assets/78b96e6c-0f7c-441e-bec4-a4cd2ec35214" />
<img width="150" alt="Screenshot_1792069197" src="https://github.com/user-attachments/assets/c0db6c64-dcfb-4b52-98dc-09d92339aa12" />
<img width="150" alt="Screenshot_1792069250" src="https://github.com/user-attachments/assets/0e2123f8-e8c1-42c6-b746-73ff692b823b" />

## Features

- **Search** for books by title or author, 20 results per page.
- **Book details** with cover, author, year, page count and a short description.
- **Your bookshelf** with all saved books in alphabetical order, filtered by title or author.
- **Read status** that you can change from the bookshelf or from the book itself.
- **Offline access** to saved books and their covers.
- **English and Swedish**, following the language of the device.
- **Light and dark mode**, following the system setting.

## Screens

### Search

Type a title or an author to search the book database. Tap a book to open it.

<img width="150" alt="Screenshot_1792069352" src="https://github.com/user-attachments/assets/17d8dd38-b8b7-4787-9dd0-d82e741c6b5a" />
<img width="150" alt="Screenshot_1792069197" src="https://github.com/user-attachments/assets/d01b9f9d-3278-474a-b921-7a95eb5a93fb" />

### Book details

See the cover, author, year, page count and description. Save the book to your bookshelf, mark it as read, or remove it again.

<img width="150" alt="Screenshot_1792069326" src="https://github.com/user-attachments/assets/86e14a23-c599-4b56-92ef-1d3f6a2c658e" />
<img width="150" alt="Screenshot_1792069214" src="https://github.com/user-attachments/assets/93ff04ed-f2be-42f4-8af1-7af1bd435501" />

### Bookshelf

All the books you have saved, with how many you have read. Filter the list by title or author, and mark books as read directly in the list.

<img width="150" alt="Screenshot_1792069318" src="https://github.com/user-attachments/assets/430a34ad-5095-4705-a08b-3fb8ed06e1f0" />
<img width="150" alt="Screenshot_1792069250" src="https://github.com/user-attachments/assets/cfa3eef0-b26a-4ec4-9010-233ddf8da23d" />

## Getting started

You need Android Studio and an Android phone or emulator running Android 8.0 or later.

1. Clone the repository and open the project folder in Android Studio.
2. Wait for Gradle to sync. It downloads the Java version the build needs by itself.
3. Choose a device and press Run.

No API key or account is needed.

## Built with

Kotlin and Jetpack Compose with Material 3, Room for the saved books, and Retrofit for talking to Open Library.

## How this app was built with AI agents

The app was built together with AI coding agents, with me in charge of every step:

- **I set the rules.** I told the agent how to write the plan for the app and its documentation. The result is AGENTS.md and the rule files in docs/agents/, which describe what the app does and how the code should be written and tested.
- **The agent plans, I approve.** For almost every step, the agent first proposed a plan. I went through it and approved it before any code was written.
- **I review before merging.** Before each pull request, I went through and reviewed the code myself.

## For contributors

The rules for how the code is written, tested and organized are in [AGENTS.md](AGENTS.md) and [docs/agents/](docs/agents/). The current progress is in [docs/roadmap.md](docs/roadmap.md), and the design images are in [docs/design/](docs/design/).

Common commands:

```bash
./gradlew assembleDebug   # build the app
./gradlew test            # run the unit tests
./gradlew lint            # run Android lint
```
