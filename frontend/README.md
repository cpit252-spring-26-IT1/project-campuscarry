# CertifiedCarry Frontend

## Overview

This frontend is the client application for CertifiedCarry, an esports talent discovery platform. It is designed around clear separation between:

- route-level page orchestration
- reusable UI components
- shared app state through React Context
- domain and API logic inside service modules

the frontend is structured to keep rendering concerns, shared state, and backend communication separate.

## Main User Flows

The frontend supports these core system behaviors:

- user authentication and session restoration
- email-verification-based signup completion
- role-aware navigation for `PLAYER`, `RECRUITER`, and `ADMIN`
- player profile creation, editing, and verification submission
- recruiter onboarding and approval tracking
- leaderboard browsing
- player discovery for recruiters
- authenticated chat between platform users
- admin moderation for recruiter and rank verification queues
- account settings such as chat privacy and recruiter DM openness
- bilingual Arabic/English presentation
- light/dark theme switching

## Architectural Structure

```text
src/
  components/   reusable UI and extracted page sections
  constants/    fixed content, translations, and app configuration
  context/      app-wide state providers
  hooks/        reusable context and flow hooks
  layouts/      shared shell and route container
  pages/        route-level orchestration components
  patterns/     explicit observer pattern implementation
  services/     API access and client-side domain logic
  utils/        presentation helpers
```

### Architectural Roles

- `App.jsx` centralizes route definitions.
- `layouts/MainLayout.jsx` provides the shared shell.
- `context/*` holds cross-cutting application state.
- `services/*` encapsulates network calls and business-oriented client logic.
- `pages/*` coordinate data loading and screen-specific behavior.
- `components/*` focus on rendering and localized UI interaction.

This structure reduces coupling and makes it easier to explain where each responsibility lives during a manual review.

## Technology Stack

- React 19
- Vite 8
- React Router
- HeroUI
- Tailwind CSS
- DaisyUI
- Axios
- Firebase Web SDK
- React Toastify

## Setup and Execution

### Requirements

- Node.js 20+ recommended
- npm
- running backend API

### Install Dependencies

```bash
npm install
```

### Environment Configuration

Create `.env` from `.env.example`.

Typical values:

```bash
VITE_API_URL=/api
VITE_API_TIMEOUT_MS=15000
VITE_DEV_PROXY_TARGET=http://localhost:8000

VITE_FIREBASE_API_KEY=...
VITE_FIREBASE_AUTH_DOMAIN=...
VITE_FIREBASE_PROJECT_ID=...
VITE_FIREBASE_STORAGE_BUCKET=...
VITE_FIREBASE_MESSAGING_SENDER_ID=...
VITE_FIREBASE_APP_ID=...
VITE_FIREBASE_MEASUREMENT_ID=...
```

### Run in Development

```bash
npm run dev
```

### Build for Production

```bash
npm run build
```

### Preview Production Build

```bash
npm run preview
```

### Lint

```bash
npm run lint
```

## Important Technical Decisions

### 1. Shared HTTP Client

- File: `src/services/httpClient.js`
- The app uses one shared Axios client.
- Firebase tokens are attached automatically through an interceptor.

Why this matters:

- request behavior stays consistent
- auth headers are not duplicated across services
- API concerns stay outside page components

### 2. Context-Based Global State

- Files: `src/context/AuthProvider.jsx`, `LanguageProvider.jsx`, `ThemeProvider.jsx`
- Authentication, language, and theme are treated as application-wide concerns.

Why this matters:

- avoids prop drilling
- keeps shared state centralized
- makes hooks like `useAuth`, `useLanguage`, and `useTheme` simple for components to consume

### 3. Service-Layer Frontend Logic

- Files: `src/services/authService.js`, `chatService.js`, `playerService.js`, `adminService.js`
- API calls and domain-oriented client logic live in service modules instead of page components.

Why this matters:

- page files stay focused on orchestration and UI
- network logic becomes reusable
- error handling is easier to standardize

### 4. Page Composition

- Files: `src/pages/DashboardPage.jsx`, `ChatsPage.jsx`, `AdminPanelPage.jsx`, `ProfileSetupPage.jsx`
- Large pages are composed from focused subcomponents rather than one monolithic JSX file.

Why this matters:

- rendering logic is easier to review
- responsibilities are narrower
- reuse becomes easier

## Implemented Design Patterns

### Creational Pattern: Singleton

- **Where:** `src/services/httpClient.js`, `src/patterns/observer/AuthEventSubject.js`
- **What it is:** one shared Axios client instance and one shared auth-event subject instance are reused across the application.
- **Why it is useful here:** the frontend needs consistent request behavior and a single auth-event source rather than many competing instances.

### Structural Pattern: Composite

- **Where:** `src/pages/DashboardPage.jsx`, `src/pages/ChatsPage.jsx`, `src/pages/AdminPanelPage.jsx`, `src/pages/ProfileSetupPage.jsx`, plus extracted component folders under `src/components/`
- **What it is:** large screens are assembled from smaller components such as dashboard blocks, chat panels, admin tabs, and profile form sections.
- **Why it is useful here:** it keeps complex pages readable and lets each child component focus on one part of the screen.

### Behavioral Pattern: Observer

- **Where:** `src/patterns/observer/Subject.js`, `Observer.js`, `AuthEventSubject.js`, with usage in `src/context/AuthProvider.jsx` and `src/layouts/MainLayout.jsx`
- **What it is:** auth actions emit events, and interested listeners subscribe and react without tight coupling.
- **Why it is useful here:** auth-related side effects can be added without hardwiring them into every auth consumer.

### `src/App.jsx`

- central route registry
- distinguishes public, public-only, and protected routes
- shows that routing policy is centralized rather than scattered

### `src/context/AuthProvider.jsx`

- main auth state container
- restores the session
- exposes login, register, logout, and account deletion flows
- emits auth events through the observer subject

### `src/services/authService.js`

- frontend auth facade
- coordinates Firebase auth, backend signup completion, and session reporting

### `src/components/ProtectedRoute.jsx`

- role-aware route guard
- demonstrates access control at the UI boundary

### `src/services/chatService.js`

- chat facade
- combines lookup, normalization, and API mutation logic into one public surface for the chat UI

## Backend Contract References

- `docs/api-contract.md`
- `docs/openapi.yaml`
- `docs/legal-consent-contract.md`

## Review Notes

- routing is centralized
- app-wide state is centralized in context providers
- service modules isolate API logic from UI logic
- large pages are decomposed into smaller render components
- there is a real explicit Observer pattern implementation in the codebase
