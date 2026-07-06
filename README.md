# 📱 Study Crossing · 学霸森友会

> **Animal Crossing-themed educational Android game** — Kotlin · SQLite · Gamification · 21 screens

---

## 🎮 Game Overview

Study Crossing is a **gamified learning platform** built as a native Android app. Inspired by Animal Crossing's cozy aesthetic, it turns academic study into an engaging island adventure where students complete quests, earn coins, collect badges, and interact with NPC characters — all while practicing English, Math, and General Knowledge.

---

## 🧱 Architecture

```
┌──────────────────────────────────────────────────────────┐
│                   Android App Architecture               │
├────────────┬─────────────┬──────────────┬────────────────┤
│  Activity  │  Adapter    │  Data Layer  │  Services      │
│  Layer     │  Layer      │              │                │
│            │             │              │                │
│  21        │  Recycler   │  Models      │  MusicManager  │
│  Activities│  Adapters   │  (Data Class)│  Utils         │
│            │             │              │                │
│  Login     │  Class      │  Database    │  MyApp         │
│  Main      │  Lesson     │  Helper      │  (Application) │
│  Study     │  Student    │  (7 tables)  │                │
│  Reward    │  Task       │              │                │
│  Profile   │  Session    │  SQLite      │  Top/Bottom    │
│  Badges    │  Teacher    │              │  Fragments     │
│  ...       │  ...        │              │                │
└────────────┴─────────────┴──────────────┴────────────────┘
```

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|-----------|
| **Language** | Kotlin |
| **UI** | Android XML Layouts · RecyclerView · Fragments · Custom Views |
| **Database** | SQLite via `SQLiteOpenHelper` (7 tables) |
| **State** | SharedPreferences · Custom DataStore classes |
| **Audio** | MediaPlayer · SoundPool (background music + SFX) |
| **Architecture** | Activity-based navigation with Fragment composition |
| **Build** | Gradle (Kotlin DSL) |

---

## ✨ Features

### 👥 3 User Roles
| Role | Capabilities |
|------|-------------|
| **Student** | Complete quests, earn coins, collect badges, view progress, take notes |
| **Teacher** | Create/manage classes, assign lessons, view student progress |
| **Admin** | Manage all users (students + teachers), edit lessons, system oversight |

### 🎯 4 Mini-Game Types
| Game | Mechanic | Subjects |
|------|----------|----------|
| **Slot Machine** | Spin to match subject-concept pairs | English, Math, General Knowledge |
| **Word Track** | Trace vocabulary words on screen | English |
| **Fossil Excavation** | Tap to dig up answers from hidden blocks | General Knowledge, Math |
| **Multiple Choice** | Timed question with 4 options | All subjects |

### 🏝️ Island Theme
- **NPC interactions** — each lesson has an Animal Crossing-style character with dialogue
- **Coin economy** — earn coins for completing quests, spend on rewards
- **Badge system** — achievement badges for milestones
- **Profile customization** — avatars and profile cards

### 📚 Learning System
- **14 quests** across 3 subjects (English, Math, General Knowledge)
- **Lesson content**: title, description, NPC dialogue, text, images, questions
- **Progress tracking** — per-student completion records
- **Notes** — students can write and save study notes per quest
- **Session records** — graded performance history

### 🛠️ Admin & Teacher Tools
- **Class management** — create classes, assign students
- **Lesson editor** — create/edit lesson content, questions, answers
- **Student progress dashboard** — view completion rates and grades
- **User management** — register, edit, manage student and teacher accounts

---

## 🗄️ Database Schema (SQLite — 7 Tables)

| Table | Key Fields | Purpose |
|-------|-----------|---------|
| `students` | id, username, password, name, form, coins, avatar | Student accounts + game economy |
| `staff` | id, username, password, name, role | Teacher + Admin accounts |
| `classes` | id, name | Class/group organization |
| `lessons` | id, title, subject, description, lesson_text, npc_name, npc_dialogue, image_uri, question_text, answer, options, game_type | Learning content + NPC metadata |
| `progress` | id, student_id, quest_id | Quest completion tracking |
| `sessions` | id, student_id, quest_id, subject, coins, grade | Graded attempt history |
| `notes` | id, student_id, quest_id, note_text | Student study notes |

---

## 🗂️ Project Structure

```
study-crossing-android/
├── app/src/main/
│   ├── java/com/example/assignment1/
│   │   ├── DatabaseHelper.kt          # SQLite: 7 tables, CRUD operations
│   │   ├── AdminDataStore.kt          # Admin-level data access
│   │   ├── IslandDataStore.kt         # Game state management
│   │   ├── MusicManager.kt            # Background music + SFX
│   │   ├── Utils.kt                   # Shared utility functions
│   │   ├── MyApp.kt                   # Application class
│   │   ├── data/model/                # Data classes (10 models)
│   │   │   ├── Student.kt, Staff.kt, Lesson.kt, Quest.kt
│   │   │   ├── SessionRecord.kt, Subject.kt, GameType.kt
│   │   │   ├── GameData.kt, LearningContent.kt, MediaItem.kt
│   │   ├── LoginActivity.kt           # Auth: student + staff login
│   │   ├── RegisterActivity.kt        # New student registration
│   │   ├── MainActivity.kt            # Student home / island hub
│   │   ├── StudyActivity.kt           # Quest gameplay screen
│   │   ├── LessonNoteActivity.kt      # Study notes with lesson content
│   │   ├── RewardActivity.kt          # Coin shop + rewards
│   │   ├── BadgesActivity.kt          # Achievement badges
│   │   ├── ProfileActivity.kt         # Student profile + stats
│   │   ├── NoticeBoardActivity.kt     # Notifications + announcements
│   │   ├── QuestArchiveActivity.kt    # Completed quest history
│   │   ├── SettingsActivity.kt        # App settings
│   │   ├── TeacherActivity.kt         # Teacher dashboard
│   │   ├── TeacherClassesActivity.kt  # Class list management
│   │   ├── TeacherClassStudentsActivity.kt  # Student roster
│   │   ├── AdminActivity.kt           # Admin dashboard
│   │   ├── AdminStudentsActivity.kt   # Student management
│   │   ├── AdminTeachersActivity.kt   # Teacher management
│   │   ├── AdminLessonsActivity.kt    # Lesson management
│   │   ├── AdminEditLessonActivity.kt # Lesson editor
│   │   ├── AdminEditStudentActivity.kt# Student editor
│   │   ├── *Adapter.kt               # 8 RecyclerView adapters
│   │   ├── TopFragment.kt             # Top navigation fragment
│   │   └── BottomFragment.kt          # Bottom navigation fragment
│   ├── res/
│   │   ├── drawable/                  # 100+ custom art assets (NPCs, items, UI)
│   │   ├── layout/                    # XML layouts per activity
│   │   └── values/                    # Strings, colors, themes
├── app/build.gradle.kts               # Gradle build config
└── app/proguard-rules.pro             # ProGuard rules
```

---

## 🎨 Game Screens (21 Total)

| # | Screen | Purpose |
|---|--------|---------|
| 1 | Splash | Animated splash with game logo |
| 2 | Login | Student / Staff authentication |
| 3 | Register | New student signup with form selection |
| 4 | Main | Student island hub with NPCs + quest access |
| 5 | Study | Quest gameplay (4 mini-game types) |
| 6 | Lesson Note | Lesson content + student notes |
| 7 | Reward | Coin shop for in-game items |
| 8 | Badges | Achievement collection view |
| 9 | Profile | Student stats, coins, level |
| 10 | Notice Board | System announcements |
| 11 | Quest Archive | Completed quest history |
| 12 | Settings | Sound, notifications, about |
| 13 | Teacher | Teacher dashboard |
| 14 | Teacher Classes | Class list |
| 15 | Teacher Class Students | Student roster per class |
| 16 | Admin | Admin control panel |
| 17 | Admin Students | All students management |
| 18 | Admin Teachers | All teachers management |
| 19 | Admin Lessons | Lesson library management |
| 20 | Admin Edit Lesson | Lesson content editor |
| 21 | Admin Edit Student | Student account editor |

---

## 🎥 Demo Video

🔗 **[Watch Demo (Google Drive)](https://drive.google.com/drive/folders/13CheMPt1-2ONk62PxLU8EUoKi822PbTU?usp=sharing)**

---

## 🚀 Getting Started

```bash
# Open in Android Studio
File -> Open -> select study-crossing-android/

# Build & Run
./gradlew assembleDebug
# or use Android Studio's Run button

# Min SDK: 24 (Android 7.0)
# Target SDK: 34
```

---

<p align="center">
  <i>Kotlin · Android · SQLite · Gamification · 21 Screens</i>
</p>
