# SACCO Mobile Banking App

Android mobile banking app for your Django SACCO MIS. Built with Kotlin, MVVM architecture, Retrofit, and JWT authentication.

---

## Project Structure

```
SaccoApp/
├── app/src/main/java/com/sacco/app/
│   ├── data/
│   │   ├── api/           # Retrofit client, API service, Auth interceptor
│   │   ├── models/        # Kotlin data classes matching your Django serializers
│   │   └── repository/    # SaccoRepository (single source of truth)
│   ├── ui/
│   │   ├── auth/          # Splash, Login, Register + AuthViewModel
│   │   ├── dashboard/     # MainActivity (bottom nav), DashboardFragment
│   │   ├── transactions/  # TransactionsFragment + adapters
│   │   ├── statement/     # StatementFragment + PDF generation
│   │   └── profile/       # ProfileFragment
│   └── utils/             # Resource, TokenManager, Extensions, PdfGenerator
└── app/src/main/res/
    ├── layout/            # All XML layouts
    ├── values/            # colors, strings, themes
    ├── menu/              # Bottom navigation menu
    └── xml/               # FileProvider paths, backup rules
```

---

## Setup in Android Studio

### 1. Import the project
File → Open → select the `SaccoApp` folder → OK

### 2. Sync Gradle
Android Studio will prompt to sync. Click **Sync Now**.

### 3. Configure your backend URL

In `app/build.gradle`, the emulator URL is already set:
```groovy
buildConfigField "String", "BASE_URL", '"http://10.0.2.2:8000/api/"'
```
- **Emulator**: `10.0.2.2:8000` (already configured)
- **Physical device on LAN**: change to your machine's local IP e.g. `http://192.168.1.100:8000/api/`
- **Production**: update the `release` buildConfigField

### 4. Django backend requirements

Ensure these are installed and configured in your Django project:
```
djangorestframework
djangorestframework-simplejwt
django-cors-headers
```

Ensure your `urls.py` has:
```python
path('api/auth/token/', TokenObtainPairView.as_view()),
path('api/auth/token/refresh/', TokenRefreshView.as_view()),
path('api/auth/register/', register_view),
path('api/auth/user/', current_user_view),
path('api/auth/logout/', logout_view),
path('api/customers/me/', customer_profile_view),
path('api/balances/summary/', balance_summary_view),
path('api/balances/savings/', savings_balance_view),
path('api/balances/loan/', loan_balance_view),
path('api/savings-transactions/', SavingsTransactionViewSet.as_view({'get': 'list'})),
path('api/loan-transactions/', LoanTransactionViewSet.as_view({'get': 'list'})),
path('api/loan-history/', LoanHistoryViewSet.as_view({'get': 'list'})),
```

### 5. Run on emulator
- Select an AVD (API 24+)
- Click ▶ Run

---

## Features

| Screen | Description |
|--------|-------------|
| **Splash** | Auto-routes to Login or Dashboard based on saved JWT |
| **Login** | JWT auth → tokens stored securely in DataStore |
| **Register** | Creates user + customer profile via your API |
| **Dashboard** | Savings/Loan/Net balances + 10 recent transactions |
| **Transactions** | Tabbed view — Savings tab and Loans tab with full history |
| **Statement** | Account summary + download full PDF statement |
| **Profile** | User/customer details, logout with confirmation |

---

## Architecture

- **MVVM** — ViewModel + LiveData, no direct UI→Repository calls
- **Repository pattern** — all API calls go through `SaccoRepository`
- **Coroutines** — async network calls, no RxJava
- **DataStore** — secure JWT persistence (replaces SharedPreferences)
- **Resource<T>** — sealed class for Loading/Success/Error states

---

## Production Checklist

- [ ] Replace `BASE_URL` in release buildConfig with your HTTPS production URL
- [ ] Remove `android:usesCleartextTraffic="true"` from manifest (only needed for HTTP)
- [ ] Enable ProGuard in release builds
- [ ] Add token refresh logic (intercept 401 → call `/auth/token/refresh/`)
- [ ] Add payment simulation endpoint to Django and wire up in the app
- [ ] Sign the APK with your release keystore
