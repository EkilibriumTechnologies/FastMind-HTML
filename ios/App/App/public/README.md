# 🧠 FastMind – AI Fasting Coach

![HTML5](https://img.shields.io/badge/HTML5-orange?logo=html5&logoColor=white)
![TailwindCSS](https://img.shields.io/badge/TailwindCSS-blue?logo=tailwindcss&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-yellow?logo=firebase&logoColor=white)
![Gemini AI](https://img.shields.io/badge/Google%20Gemini-AI-blueviolet?logo=google&logoColor=white)
![Netlify](https://img.shields.io/badge/Deployed%20on-Netlify-brightgreen?logo=netlify&logoColor=white)

**FastMind** is an intelligent web application designed to **guide, track, and educate users** throughout their intermittent fasting journey.  
It combines a **real-time fasting timer**, an **AI wellness assistant**, and **personal user accounts** to create a holistic and persistent fasting experience.

🌐 **Live App:** [https://fastmind-2.netlify.app/](https://fastmind-2.netlify.app/)  
📦 **Repository:** [GitHub – FastMind](#)

---

## 🧩 Project Description

FastMind is a **Single Page Application (SPA)** built with **HTML5**, **Tailwind CSS**, and **modern JavaScript (ESM)**.  
It integrates **Google Gemini (AI)** and **Firebase** services for authentication, secure data management, and intelligent conversational support.

---

## ⚙️ Key Features

### 1. 🔐 User Accounts & Authentication
- **Login with Google:** Secure authentication through Firebase Authentication.
- **Session Persistence:** Keeps users logged in with access to their personal dashboard.
- **Data Security:** Multi-user structure ensures each user can only access their own fasting history.

---

### 2. ⏱️ Intelligent Fasting Timer & Biological Phases
- **Interactive Start/Stop Timer:** Tracks fasting duration in real-time (hours, minutes, seconds).
- **Dynamic Circular Dial:** Visual progress indicator that fills as time passes, changing colors by biological phase.
- **Scientific Phase Tracking:** Based on a built-in CSV dataset, users see their current biological phase (e.g., _Digest_, _Burn_, _Switch_, _Cleanse_, _Renew_) for motivation and education.

---

### 3. 🤖 AI Assistant “FastMind” (Hybrid RAG System)
- **Built-in Chatbot:** Powered by **Google Gemini 2.5 Flash**, answers user questions about fasting, nutrition, and wellness.
- **Hybrid RAG (Retrieval-Augmented Generation):**
  1. **Local Knowledge Retrieval:** Uses an embedded fasting_guide.pdf knowledge base for instant validated answers.
  2. **Web Search Fallback:** When the local base can’t answer, Gemini automatically performs a Google Search.
- **Source Citations:** When using Google Search, the chatbot displays the source title and URL for transparency.

---

### 4. 🗂️ Personalized Fasting History
- **Automatic Save:** Each completed fast is saved when pressing “Stop”.
- **Cloud Database:** Stored in **Firebase Firestore** with strict access rules  
  (`/users/{userId}/fasts/{fastId}`).
- **History Visualization:** “My History” tab lists all previous fasts sorted by date, including:
  - Duration  
  - Date  
  - Final Phase Reached  

---

## 🧱 Technical Architecture

| Layer | Technology |
|-------|-------------|
| **Frontend** | HTML5, Tailwind CSS, JavaScript (ES Modules) |
| **AI Model** | Google Gemini 2.5 Flash |
| **Authentication** | Firebase Authentication |
| **Database** | Firebase Firestore |
| **RAG Engine** | Hybrid Local + Google Search via Gemini API |
| **Deployment** | GitHub + Netlify (CI/CD integrated) |

---

## 🚀 Installation & Setup

### 1. Clone the repository
```bash
git clone https://github.com/<your-username>/fastmind.git
cd fastmind
2. Open in your browser

You can open index.html directly to preview the app locally.

For local testing with Firebase:

Create a Firebase project.

Enable Google Authentication.

Add your Firebase config to the JavaScript initialization section.

3. Add your Gemini API Key

Replace your API key in the configuration:

const GEMINI_API_KEY = "AIzaSy..."; // Replace with your own

🧠 Future Improvements

✅ Add user accounts with fasting history tracking (implemented)

🔄 Add streaks and analytics dashboard

📊 Export fasting data as CSV

📱 Convert to full PWA (Progressive Web App)

💬 Integrate voice chat with Gemini

