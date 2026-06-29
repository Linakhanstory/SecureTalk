
SecureTalk is a professional, real-time communication platform designed to bridge the gap between interns and mentors. Built with modern Android development practices, it ensures seamless collaboration, instant messaging, and end-to-end data security.

Key Features
Real-Time Messaging: Instant delivery for 1-on-1 and group conversations using Firebase Firestore.

Enhanced Privacy: End-to-End Encryption (E2EE) implementation to protect sensitive intern-mentor communications.

Engagement Tools: Features typing indicators and read receipts to provide a professional chat experience.

Modern UI: Built with Jetpack Compose following Material Design 3 guidelines for a clean, intuitive interface.

Secure Authentication: Firebase Authentication integrated for reliable user identity management.

Tech Stack
Language: Kotlin

UI Framework: Jetpack Compose (Material Design 3)

Backend/Database: Firebase Firestore

Authentication: Firebase Auth

Security: Google Tink (for E2EE)

Architecture: MVVM (Model-View-ViewModel)



Firebase Configuration:

Go to the Firebase Console.

Add your Android app and download the google-services.json file.

Place this file inside the app/ folder of the project.

Build & Run:

Sync the project with Gradle in Android Studio.

Build and run on an emulator or physical device.

Security Note
This platform is built with privacy in mind. All messages stored in the database are encrypted, ensuring that only the intended recipients can read the content of the communication.

Contributing
Contributions are welcome! If you'd like to improve the encryption or add new collaborative features, please fork the repository and submit a pull request.
