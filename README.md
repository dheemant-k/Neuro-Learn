# NeuroLearn

## Adaptive, AI-Assisted Learning Platform

NeuroLearn is an end-to-end adaptive learning platform designed to personalize a student's learning experience based primarily on their **learning performance and historical progress**.

The project is being developed jointly for **Software Engineering** and **Enterprise Web Application** coursework.

The primary goal is to build a **reliable, deployable full-stack application** using Java/Spring Boot, React, and PostgreSQL, with AI-assisted question generation and an explainable adaptive learning engine.

> **Core principle:** Understand how a student is performing → adapt the difficulty → track their progress → provide useful insights.

---

# 1. Problem Statement

Most digital learning platforms provide students with a relatively fixed sequence of questions or content.

However, students differ in:

- Existing knowledge
- Learning speed
- Accuracy
- Response time
- Topic proficiency
- Ability to handle increasing difficulty

A fixed learning path may therefore be too easy for some students and too difficult for others.

**NeuroLearn addresses this by dynamically adapting the difficulty and topic of questions according to the student's demonstrated performance.**

---

# 2. Objectives

The primary objectives are:

1. Build a complete full-stack adaptive learning platform.
2. Dynamically adjust question difficulty based on student performance.
3. Maintain a persistent learning profile for every student.
4. Provide separate dashboards for students, teachers, and parents.
5. Use AI to assist in generating and expanding the question bank.
6. Demonstrate enterprise web application concepts using Java and Spring Boot.
7. Provide an explainable and measurable adaptive learning mechanism.

---

# 3. Core Concept

The fundamental NeuroLearn learning loop is:

```text
Student
   ↓
Receives Question
   ↓
Solves Question
   ↓
Performance Recorded
   ↓
Performance Analysed
   ↓
Adaptive Engine
   ↓
Determine Next Difficulty
   ↓
Select Next Question
   ↓
Update Learning Profile
   ↓
Repeat
```

The system does **not** require an AI API call to generate every question in real time.

Questions are stored in a database and selected by the adaptive engine, ensuring that the learning experience remains responsive.

---

# 4. Adaptive Learning Engine

The adaptive engine is the **core intelligence of NeuroLearn**.

It analyses measurable learning signals such as:

- Correctness
- Recent accuracy
- Response time
- Number of attempts
- Consecutive correct answers
- Consecutive incorrect answers
- Current question difficulty
- Topic-wise performance
- Historical learning performance

Based on these signals, the system determines whether the student should:

```text
↑ Increase Difficulty

→ Maintain Difficulty

↓ Decrease Difficulty
```

### Example

A student answers several Level 2 questions correctly with reasonable response times:

```text
High Accuracy
+
Good Response Time
+
Consistent Performance
        ↓
Increase Difficulty
        ↓
Level 3 Question
```

A student repeatedly struggles:

```text
Low Accuracy
+
Repeated Mistakes
+
High Response Time
        ↓
Decrease Difficulty
        ↓
Level 1 Question
```

The initial adaptive engine will be **rule-based and explainable** rather than completely dependent on a machine-learning model.

---

# 5. Performance Analysis

Every question attempt generates learning data.

Example:

```text
Student: S001
Topic: Algebra
Difficulty: 2
Correct: Yes
Response Time: 7.4 seconds
Timestamp: ...
```

The system aggregates these interactions to calculate:

- Recent accuracy
- Overall accuracy
- Topic mastery
- Average response time
- Difficulty progression
- Number of attempts
- Learning consistency

This information contributes to the student's persistent learning profile.

---

# 6. Personalized Learning Profile

Each student has a continuously updated profile.

Conceptually:

```text
Student
│
├── Overall Performance
│
├── Topic Performance
│     ├── Algebra
│     ├── Geometry
│     └── Calculus
│
├── Current Difficulty
│
├── Average Response Time
│
├── Learning History
│
└── Progress
```

This allows NeuroLearn to personalize future learning sessions based on previous performance.

For example:

```text
Previous Sessions
       ↓
Topic Performance
       ↓
Current Proficiency
       ↓
Starting Difficulty
       ↓
New Learning Session
```

---

# 7. Question Bank

Questions will be stored in a structured question bank.

Each question can contain:

```text
Question
├── ID
├── Topic
├── Difficulty
├── Question Text
├── Option A
├── Option B
├── Option C
├── Option D
├── Correct Answer
└── Explanation
```

Questions will be categorized by:

- Subject
- Topic
- Difficulty
- Question type

The adaptive engine retrieves an appropriate question based on the student's current learning state.

---

# 8. AI-Assisted Question Generation

An LLM will be used to assist in expanding the question bank.

The system can provide the model with:

```text
Topic
Difficulty
Question Type
Learning Objective
```

The model generates:

```text
Question
Options
Correct Answer
Explanation
```

The generated content can then be reviewed/validated and stored in the question bank.

### Important Design Decision

LLM generation is **not part of the real-time question delivery loop**.

Instead:

```text
Topic + Difficulty
        ↓
    LLM API
        ↓
Generate Questions
        ↓
Validate
        ↓
Store in Database
        ↓
Question Bank
```

During learning:

```text
Student
   ↓
Adaptive Engine
   ↓
Question Bank
   ↓
Next Question
```

This avoids API latency and ensures the platform continues functioning even if the LLM service is temporarily unavailable.

---

# 9. Dashboards

NeuroLearn will provide different interfaces according to the user's role.

## 9.1 Student Dashboard

The student dashboard provides:

- Overall progress
- Current learning level
- Accuracy
- Topic-wise performance
- Strong and weak topics
- Learning history
- Difficulty progression
- XP/streaks/achievements
- Recent activity

The student can answer:

> **"How am I performing and what should I work on next?"**

---

## 9.2 Teacher Dashboard

The teacher dashboard provides an aggregated view of student performance.

It can include:

- Student performance
- Topic-wise weaknesses
- Progress over time
- Difficulty progression
- Learning activity
- Accuracy trends
- Students requiring additional attention

The teacher can answer:

> **"How are my students performing and where do they need help?"**

---

## 9.3 Parent Dashboard

The parent dashboard provides a simplified overview:

- Overall progress
- Learning consistency
- Strengths
- Weak areas
- Recent learning activity
- General performance trends

The parent can answer:

> **"Is the student progressing consistently?"**

---

# 10. Gamification

Gamification will be used as a supporting feature to encourage consistent learning.

Potential features include:

- XP
- Levels
- Streaks
- Achievements
- Daily goals
- Progress indicators
- Topic mastery

Gamification is secondary to the adaptive learning system and will only be expanded after the core learning loop is stable.

---

# 11. Authentication and Role-Based Access

The application will support different user roles.

```text
                 User
                   │
        ┌──────────┼──────────┐
        ↓          ↓          ↓
     Student    Teacher     Parent
```

Each role will have appropriate permissions and dashboard access.

For example:

```text
Student
→ Own learning data

Teacher
→ Assigned student/class data

Parent
→ Associated student's progress
```

Spring Security will be used for authentication and authorization.

---

# 12. System Architecture

The planned architecture is:

```text
                         ┌──────────────────┐
                         │   React Client   │
                         └────────┬─────────┘
                                  │
                              REST APIs
                                  │
                                  ▼
                    ┌─────────────────────────┐
                    │      Spring Boot        │
                    │        Backend          │
                    ├─────────────────────────┤
                    │ Authentication          │
                    │ User Management         │
                    │ Question Management      │
                    │ Learning Sessions       │
                    │ Performance Analysis    │
                    │ Adaptive Engine          │
                    │ AI Integration           │
                    │ Analytics                │
                    └────────────┬────────────┘
                                 │
                                 ▼
                         ┌──────────────┐
                         │  PostgreSQL  │
                         │   Database   │
                         └──────────────┘
                                 ▲
                                 │
                         ┌───────┴───────┐
                         │               │
                    Question Bank    User Data
                         ▲
                         │
                    LLM Generation
```

---

# 13. Technology Stack

## Frontend

**React**

Responsibilities:

- Learning interface
- Student dashboard
- Teacher dashboard
- Parent dashboard
- Authentication interface
- Gamification UI
- API integration

## Backend

**Java + Spring Boot**

Responsibilities:

- REST APIs
- Business logic
- Authentication
- Authorization
- User management
- Question management
- Learning sessions
- Performance analysis
- Adaptive engine
- Analytics
- AI service integration

## Database

**PostgreSQL**

Stores:

- Users
- Roles
- Student profiles
- Questions
- Question attempts
- Learning sessions
- Performance metrics
- Topic progress
- Gamification data

## AI

**LLM API**

Used primarily for:

- Question generation
- Question variations
- Explanations where appropriate

AI generation is asynchronous and decoupled from the real-time learning loop.

---

# 14. Database Design

A simplified relational model:

```text
USER
 │
 ├── ROLE
 │
 └── STUDENT_PROFILE
          │
          ├── LEARNING_SESSION
          │       │
          │       └── QUESTION_ATTEMPT
          │                    │
          │                    └── QUESTION
          │
          ├── TOPIC_PROGRESS
          │
          └── GAMIFICATION_DATA
```

### Core Entities

#### User

```text
id
name
email
password
role
created_at
```

#### Student Profile

```text
id
user_id
current_level
overall_accuracy
total_xp
```

#### Question

```text
id
topic
difficulty
question_text
option_a
option_b
option_c
option_d
correct_answer
explanation
```

#### Question Attempt

```text
id
student_id
question_id
correct
response_time
attempt_number
timestamp
```

#### Learning Session

```text
id
student_id
start_time
end_time
questions_attempted
accuracy
```

---

# 15. End-to-End Functioning

The complete user journey is:

```text
                 LOGIN
                   │
                   ▼
          Authenticate User
                   │
                   ▼
          Identify User Role
                   │
                   ▼
          Student Dashboard
                   │
                   ▼
         Start Learning Session
                   │
                   ▼
        Load Student Profile
                   │
                   ▼
        Determine Initial Level
                   │
                   ▼
           Select Question
                   │
                   ▼
          Student Answers
                   │
                   ▼
        Record Question Attempt
                   │
                   ▼
        Calculate Performance
                   │
                   ▼
          Adaptive Engine
                   │
          ┌────────┼────────┐
          ▼        ▼        ▼
       Increase  Maintain  Decrease
       Difficulty Difficulty Difficulty
          │        │        │
          └────────┼────────┘
                   ▼
           Select Next Question
                   │
                   ▼
          Update Learning Profile
                   │
                   ▼
              Next Question
                   │
                   └───────────→ Repeat
```

At the end:

```text
Learning Session
       ↓
Persist Data
       ↓
Update Student Profile
       ↓
Update Analytics
       ↓
Update Dashboards
```

---

# 16. Data Strategy

NeuroLearn does **not** require a single dataset containing every type of information.

The project separates data into:

### Educational Interaction Data

A public educational dataset such as **ASSISTments** can be used to study student interaction and performance patterns.

This can help with:

- Performance analysis
- Adaptive algorithm development
- Historical behaviour analysis
- Evaluation

### Application Data

NeuroLearn will generate its own data during actual usage:

```text
Student
↓
Question Attempt
↓
Correctness
↓
Response Time
↓
Performance History
↓
Learning Profile
```

This data will be stored in PostgreSQL.

### Question Data

Questions will be obtained through:

- Manually curated questions
- AI-assisted question generation
- Question validation

and stored in the NeuroLearn question bank.

---

# 17. Engagement Model — Future Extension

**An engagement/attention model is NOT part of the current project scope.**

The current version of NeuroLearn will **not** depend on:

- Webcam processing
- MediaPipe
- Facial landmarks
- Gaze detection
- LSTM engagement models
- Facial attention datasets

The current adaptive engine is based on **student learning performance**.

A future version could explore:

```text
Performance Signals
        +
Engagement Signals
        ↓
Adaptive Engine
```

However, this will only be considered after the core system has been successfully completed.

This separation ensures that the lack of a suitable engagement dataset or model does not affect the delivery of the core project.

---

# 18. Scope Definition

## Guaranteed Deliverables

### Core Platform

- [ ] React frontend
- [ ] Java Spring Boot backend
- [ ] PostgreSQL database
- [ ] Authentication
- [ ] Role-based authorization
- [ ] Student profile
- [ ] Question bank
- [ ] Learning sessions
- [ ] Question attempts
- [ ] Performance tracking
- [ ] Adaptive difficulty engine
- [ ] Persistent learning history
- [ ] Student dashboard
- [ ] Teacher dashboard
- [ ] Parent dashboard
- [ ] REST API integration
- [ ] Deployment
- [ ] Testing

### AI

- [ ] LLM-assisted question generation
- [ ] Stored/cached generated questions
- [ ] Integration with question bank

---

## Stretch Features

Only after the core system is stable:

- [ ] Advanced gamification
- [ ] Advanced analytics
- [ ] More sophisticated adaptive algorithms
- [ ] Additional question types
- [ ] Real-time teacher updates
- [ ] Engagement/attention estimation

---

# 19. Explicitly Out of Scope

The current project will **not** attempt to build:

- ❌ ADHD diagnosis
- ❌ Medical/psychological assessment
- ❌ A fully autonomous AI tutor
- ❌ Real-time LLM-generated questions
- ❌ Real-time webcam-based attention detection
- ❌ Facial emotion recognition
- ❌ Large-scale custom deep-learning models
- ❌ A system dependent on a large proprietary dataset

These features may be considered as future work.

---

# 20. 100-Hour Development Plan

The project has a hard target of approximately **100 working hours**.

| Phase | Hours |
|---|---:|
| Planning & Architecture | 8 |
| Spring Boot Foundation | 12 |
| Authentication & Roles | 8 |
| Core Learning System | 18 |
| Adaptive Engine | 12 |
| React Frontend & Dashboards | 15 |
| AI Question Generation | 5 |
| Testing & Integration | 10 |
| Deployment & Documentation | 7 |
| **Total** | **100** |

---

# 21. Development Strategy

The project follows a strict **MVP-first** approach.

### Phase 1

Build:

```text
Database
   ↓
Spring Boot
   ↓
REST APIs
   ↓
React
```

### Phase 2

Build:

```text
Question Bank
   ↓
Learning Session
   ↓
Performance Tracking
```

### Phase 3

Build:

```text
Performance
   ↓
Adaptive Engine
   ↓
Next Difficulty
```

### Phase 4

Build:

```text
Student Dashboard
Teacher Dashboard
Parent Dashboard
```

### Phase 5

Add:

```text
LLM Question Generation
```

### Phase 6

Testing + deployment.

Only after all of the above is stable should stretch features be considered.

---

# 22. Evaluation

## Software Engineering

The project can demonstrate:

- Requirements engineering
- Use-case modelling
- Modular architecture
- Separation of concerns
- SOLID principles
- Design patterns where appropriate
- Unit testing
- Integration testing
- Version control
- Documentation

## Enterprise Web Application

The project demonstrates:

- Java
- Spring Boot
- REST APIs
- Spring Security
- JPA/Hibernate
- PostgreSQL
- Authentication
- Role-based authorization
- Frontend-backend integration
- Deployment

## Adaptive Learning

The system can be evaluated through:

- Accuracy
- Response time
- Difficulty progression
- Topic mastery
- Learning improvement
- Adaptation consistency

A useful experiment is to compare:

### Fixed Difficulty

```text
Same difficulty progression
        ↓
Student Performance
```

against:

### Adaptive Difficulty

```text
Student Performance
        ↓
Adaptive Engine
        ↓
Dynamic Difficulty
```

This gives the project a measurable technical evaluation rather than simply demonstrating that the application works.

---

# 23. Success Criteria

The minimum successful end-to-end demonstration should be:

```text
Register / Login
       ↓
Student Dashboard
       ↓
Start Learning Session
       ↓
Receive Question
       ↓
Submit Answer
       ↓
Performance Recorded
       ↓
Difficulty Adapted
       ↓
Receive Next Question
       ↓
Complete Session
       ↓
Learning Profile Updated
       ↓
Dashboard Updated
```

A teacher should be able to observe the entire process from **login to adaptive learning to persistent analytics** without manual intervention.

---

# 24. Final Project Positioning

NeuroLearn is:

> **An adaptive, AI-assisted learning platform that uses student performance and historical learning behaviour to personalize question difficulty and provide meaningful learning insights through role-specific dashboards.**

The project prioritizes:

```text
Reliability
    ↓
End-to-End Functionality
    ↓
Explainability
    ↓
AI Enhancement
    ↓
Future Research Extensions
```

The **adaptive learning engine is the core of NeuroLearn**.

AI-assisted question generation enhances the system.

Engagement/attention modeling is explicitly reserved for **future work** and is not required for the current 100-hour implementation.

---

# 25. One-Sentence Summary

> **NeuroLearn continuously learns from how a student performs, adapts the difficulty of what they see next, maintains their learning profile over time, and provides students, teachers, and parents with actionable learning insights.**