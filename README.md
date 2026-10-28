# SmartDecision - Smart School Discovery & Recommendation System

SmartDecision is a comprehensive Android-based solution designed to bridge the gap between educational institutions and students/parents in Abbottabad. It leverages a sophisticated recommendation engine to help users find schools that perfectly match their academic needs, budget, and location, while providing a robust management platform for school owners and administrators.

## 🌟 Key Features

### 1. Multi-Role Ecosystem
The application provides tailored experiences for three distinct user types:
- **User (Parents/Students)**: Discover, filter, and receive recommendations for schools. View details, check admission status, and read reviews.
- **School Owner**: Register schools, manage school profiles, update fee structures, announce admission openings, and track student interests.
- **Administrator**: Oversee the entire platform, approve/reject new school registrations, and ensure data quality.

### 2. Smart Recommendation Engine
- **Intelligent Scoring**: Schools are ranked based on a multi-factor algorithm including **User Ratings**, **Distance from Location**, and **Affordability**.
- **Comprehensive Filtering**: Narrow down choices using:
    - **Grade Levels**: From Playgroup/Nursery up to Intermediate.
    - **Dynamic Budgeting**: Real-time fee filtering with an interactive slider.
    - **Instruction Medium**: Filter by English or Urdu medium.
    - **Gender Preference**: Boys, Girls, or Co-education options.
    - **Merit Compatibility**: Match with schools based on previous academic performance.

### 3. Real-Time Admission Portal
- **Admission Status**: Live indicators for "Admission Open" or "Closed".
- **Detailed Intake Info**: View admission start/end dates, available seats for boys/girls, and admission fees.
- **Class-wise Fees**: Transparent display of monthly fees for Nursery, Primary, Middle, Matric, and Inter levels.

### 4. Interactive Map API Integration
- **Spatial Awareness**: Visualize all schools on an integrated **Map API**.
- **Distance Calculation**: Automatically calculates the distance from the user's current GPS location to each school.
- **Navigation**: Seamless one-tap integration to get driving directions.

### 5. Advanced Management & UI
- **Admin Dashboard**: Specialized interface for managing school approvals and system statistics.
- **Owner Dashboard**: Dedicated panel for owners to manage multiple school listings and monitor notifications.
- **Real-Time Notifications**: Stay updated with system alerts and approval statuses.
- **Dark/Light Mode**: Full support for dynamic theme switching for better accessibility and user comfort.

## 🛠️ Technical Stack

- **Language**: Java (Android SDK)
- **Database**: Firebase Realtime Database (with offline-sync capabilities)
- **Location Services**: Fused Location Provider (Google Play Services)
- **Maps**: **Map API** for Android
- **UI Architecture**: Material Design 3, Material Card Views, Custom Adapters, and Responsive Layouts
- **Pattern**: Model-View-Controller (MVC)

## 📁 Project Architecture

- **`activities/`**: UI controllers for all screens (Splash, Role Selection, Dashboards, Map, Preferences, etc.).
- **`models/`**: Robust data entities (`School`, `User`, `Review`, `Notification`).
- **`adapters/`**: Custom data binders for list views (School lists, Reviews, Admin Approvals).
- **`utils/`**: Core logic including `FirebaseHelper` for data aggregation and `RecommendationEngine` for the ranking algorithm.

## ⚙️ Installation & Setup

1. **Clone & Import**:
   ```bash
   git clone [repository-url]
   ```
   Open the project in **Android Studio** (Hedgehog or newer recommended).

2. **Firebase Integration**:
   - Place your `google-services.json` in the `app/` directory.
   - Enable **Realtime Database** and **Authentication** (Email/Password) in the Firebase Console.

3. **Map API Setup**:
   - Obtain an API Key from your Cloud Console.
   - Ensure `ACCESS_FINE_LOCATION` permissions are granted on the device.

4. **Run**:
   - Build the project and deploy to an Android device (API 24+ recommended).

---
**SmartDecision** - Empowering education through technology.
