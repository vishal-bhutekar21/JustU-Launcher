<div align="center">
  <h1>✨ JustU Launcher ✨</h1>
  <h3>A Minimalist, Distraction-Free Android Launcher Designed to Give You Your Time Back.</h3>
  <p>Built with ❤️ using Kotlin and Jetpack Compose</p>
  
  <p>
    <a href="https://github.com/vishal-bhutekar21/JustU-Launcher/stargazers"><img src="https://img.shields.io/github/stars/vishal-bhutekar21/JustU-Launcher?style=for-the-badge&color=yellow" alt="Stars"></a>
    <a href="https://github.com/vishal-bhutekar21/JustU-Launcher/network/members"><img src="https://img.shields.io/github/forks/vishal-bhutekar21/JustU-Launcher?style=for-the-badge&color=blue" alt="Forks"></a>
    <a href="https://github.com/vishal-bhutekar21/JustU-Launcher/issues"><img src="https://img.shields.io/github/issues/vishal-bhutekar21/JustU-Launcher?style=for-the-badge&color=red" alt="Issues"></a>
  </p>
</div>

---

## 📖 Introduction
In a world full of colorful, addictive grids of icons begging for your attention, **JustU Launcher** takes a completely different approach. It is an open-source Android home screen replacement built specifically to combat digital addiction. 

Instead of drawing you in, JustU provides a clean, text-based interface. It forces intentionality by intercepting mindless scrolling and providing a beautiful, pure black AMOLED interface to save both your battery and your focus. It's not just a launcher; it's a digital wellbeing tool.

---

## ✨ Core Features Detailed

### 🧘‍♂️ Mindful App Launching
Tapping an addictive app triggers a **5-second breathing screen** that asks *"Is this necessary?"*, forcing you to pause and think before falling down a scrolling rabbit hole. Essential apps like your Phone, Camera, and Settings bypass this delay completely.

### 📊 Reality Check Dashboard
Swipe left from the home screen to instantly view your **Usage Dashboard**. It queries the Android system to show you a ranked list of your Top 5 most-used apps for the day and exactly how many hours and minutes you've spent on them.

### 🎯 Focus Mode
A quick toggle sitting at the bottom of the home screen instantly hides all non-essential apps from your view. When activated, only tools you truly need remain on the screen.

### 🌑 Pure Black AMOLED UI
A true `#000000` AMOLED black theme that dramatically reduces eye strain, prevents screen burn-in, and extends your phone's battery life.

### 📱 Complete App Management
Swipe up to reveal your full app drawer. 
- **Long-Press** any app to pin it to your favorites list on the home screen.
- **Hide Apps**: Completely hide bloatware or distracting apps from the launcher so you never have to see them again.

### 📳 Premium UX (Haptics & Animations)
Enjoy a premium feel with subtle Android `LocalHapticFeedback` vibrations on every interaction, paired with a calming "breathing" fade animation on the home screen clock to set a mindful tone.

---

## 🛠 Detailed Tech Stack
This app was built from the ground up using modern Android development standards:
- **Language**: [Kotlin](https://kotlinlang.org/) (100%)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3) for a fully declarative UI.
- **Architecture**: **MVVM** (Model-View-ViewModel) + **Clean Architecture** ensuring separation of concerns.
- **Dependency Injection**: [Dagger Hilt](https://dagger.dev/hilt/) for scalable and testable DI.
- **Asynchronous Programming**: **Kotlin Coroutines** & **StateFlow** for reactive data handling.
- **Local Persistence**: **DataStore Preferences** replacing SharedPreferences for robust, asynchronous local storage.
- **System Integrations**: 
  - `UsageStatsManager` for the Reality Check Dashboard.
  - `DevicePolicyManager` for double-tap-to-lock screen capabilities.

---

## 🚀 How to Copy & Install (Getting Started)

Want to run this launcher on your own device? It's incredibly simple!

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (Latest Version)
- Android Device or Emulator running Android 8.0 (API 26) or higher.

### Step-by-Step Guide
1. **Clone the Repository**:
   Open your terminal and run the following command to copy the code to your local machine:
   ```bash
   git clone https://github.com/vishal-bhutekar21/JustU-Launcher.git
   ```
2. **Open in Android Studio**:
   Open Android Studio, select `Open`, and navigate to the `JustU-Launcher` folder you just cloned.
3. **Sync Gradle**:
   Wait a moment for Android Studio to index the files and download the necessary dependencies via Gradle.
4. **Run the App**:
   Connect your Android phone via USB (with USB Debugging enabled) or start an Emulator. Click the green **Run (Play)** button in the top toolbar.
5. **Set as Default**:
   When the app launches, agree to the Terms & Conditions and follow the prompt to set JustU as your Default Home App!

---

## 🤝 How to Contribute

This is an open-source project, and contributions of all kinds are highly encouraged! Whether it's fixing a bug, adding a new feature, or improving documentation, your help is welcome.

1. **Fork the Repository**: Click the "Fork" button at the top right of this page.
2. **Clone your Fork**: `git clone https://github.com/YOUR_USERNAME/JustU-Launcher.git`
3. **Create a Branch**: `git checkout -b feature/AmazingFeature`
4. **Make your Changes**: Write your code and commit your changes (`git commit -m 'Add some AmazingFeature'`).
5. **Push to the Branch**: `git push origin feature/AmazingFeature`
6. **Open a Pull Request**: Go to the original repository and click "Compare & pull request".

---

## ⭐ Support the Project!
If you find this launcher helpful in reducing your screen time, or if you simply appreciate the open-source code, **please consider giving this repository a ⭐ Star!** 
It helps the project grow and reach more developers.

---

## 👤 About the Developer & Owner

**Vishal Bhutekar**

I am a passionate Android Engineer and UI/UX Designer focused on creating impactful, user-centric applications that solve real-world problems. Let's connect!

- 💼 **LinkedIn**: [Vishal Bhutekar](https://www.linkedin.com/in/vishal-bhutekar21/)
- 🌐 **Portfolio**: [vishalbhutekar.netlify.app](https://vishalbhutekar.netlify.app/)
- 💻 **GitHub**: [vishal-bhutekar21](https://github.com/vishal-bhutekar21)
- ✉️ **Email**: [vishal.bhutekar1@gmail.com](mailto:vishal.bhutekar1@gmail.com)
- 📱 **Play Store Apps**: [Unexplored Vishal](https://play.google.com/store/apps/developer?id=Unexplored+Vishal)
- 📸 **Instagram**: [@unexplored_vish_2.0](https://www.instagram.com/unexplored_vish_2.0/)

---
*Built to give you your life back. Stop scrolling, start living.*
