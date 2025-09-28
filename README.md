# TrialAI - Courtroom AI Ethics Simulation

## Overview

TrialAI is an interactive JavaFX application that simulates a courtroom trial focusing on AI ethics and intellectual property law. Players take on the role of a jury member in a case where an AI is on trial for using a logo without proper authorization. The game explores themes of AI responsibility, ethical decision-making under pressure, and intellectual property rights.

## Features

- **Interactive Courtroom Experience**: Navigate through different phases of a trial including witness testimonies, evidence examination, and verdict deliberation
- **AI-Powered Characters**: Interact with AI witnesses and defendants that respond dynamically using OpenAI's Chat Completions API
- **Text-to-Speech Integration**: Enhanced accessibility with Google Text-to-Speech for character dialogue
- **Multiple Perspectives**: Engage with different witnesses including AI entities, human witnesses, and defendants
- **Ethical Decision Making**: Make complex moral and legal judgments based on presented evidence
- **Memory Mechanisms**: Explore flashbacks and memory reconstruction as part of the evidence gathering process

## Game Flow

1. **Courtroom Entry**: Begin in the main courtroom environment
2. **Witness Interaction**: Talk to various participants including:
   - AI witnesses (Kalani, Gregor, K2)
   - Human witnesses
   - The defendant
3. **Evidence Examination**: Review flashbacks and memory mechanisms
4. **Verdict Phase**: Make a final judgment (Guilty/Not Guilty) with rationale
5. **Feedback System**: Receive AI-generated feedback on your decision

## Technical Requirements

- **Java**: Version 21 or higher
- **JavaFX**: Included as a Maven dependency
- **Maven**: For dependency management and build processes
- **API Access**: Required for AI chat completions and text-to-speech functionality

## Setup Instructions

### 1. API Configuration for Chat Completions and TTS

Create a file named `apiproxy.config` in the root directory (same level as `pom.xml`) with your credentials:

```
email: "your-email@domain.com"
apiKey: "YOUR_API_KEY"
```

**API Token Usage:**
- Google Standard TTS: 1 credit per character
- Google WaveNet/Neural2 TTS: 4 credits per character
- OpenAI Text-to-Text: 1 credit per character
- OpenAI Chat Completions: 1 credit per token (input + output)

### 2. Running the Application

**Standard Execution:**
```bash
.\mvnw.cmd clean javafx:run
```

**Debug Mode:**
```bash
.\mvnw.cmd clean javafx:run@debug
```
Then use VS Code's "Run & Debug" → "Debug JavaFX"

## Project Structure

```
src/
├── main/
│   ├── java/nz/ac/auckland/se206/
│   │   ├── controllers/          # JavaFX controllers for each scene
│   │   ├── model/                # Data models (Participant, etc.)
│   │   ├── states/               # Game state management
│   │   ├── services/             # Utility services (Timer, etc.)
│   │   ├── speech/               # Text-to-speech integration
│   │   └── prompts/              # AI prompt engineering
│   └── resources/
│       ├── fxml/                 # JavaFX scene definitions
│       ├── css/                  # Stylesheets
│       ├── images/               # Game assets and avatars
│       ├── prompts/              # AI character prompts
│       └── data/                 # Game data (professions list)
└── test/                         # Unit tests
```

## Development Highlights

- **State Pattern Implementation**: Clean separation of game phases using the State design pattern
- **API Integration**: Seamless integration with OpenAI and Google Cloud services
- **Modern JavaFX**: Utilizes contemporary JavaFX practices with FXML and CSS styling
- **Ethical AI Focus**: Addresses current real-world concerns about AI responsibility and ethics
- **Scalable Architecture**: Well-structured codebase supporting easy expansion and modification

## Educational Value

This project demonstrates proficiency in:
- Advanced Java programming with JavaFX
- API integration and HTTP client usage
- State pattern and object-oriented design
- User interface design and user experience considerations
- Contemporary AI ethics and legal frameworks
- Maven build system and dependency management

## Development Team

This project was developed as a group assignment with the following contributors:

### John Van-As
- **AI Witness System**: Developed the interactive AI witness functionality
- **Main Room Interface**: Created the central courtroom navigation and interaction system
- **Timer Service**: Implemented game timing mechanisms and countdown functionality
- **Chat Sharing**: Built the communication system between different game components

### Ela Yildiz
- **Human Witness Module**: Developed the human witness interaction system
- **Verdict Room**: Created the final judgment interface and decision-making mechanics
- **Concurrency Management**: Implemented thread-safe operations and asynchronous processing

### Navini Ariyasinghe
- **AI Defendant System**: Built the defendant character AI and response system
- **Flashback Mechanisms**: Developed the memory reconstruction and evidence review features
- **Text-to-Speech Integration**: Implemented the TTS functionality for character dialogue