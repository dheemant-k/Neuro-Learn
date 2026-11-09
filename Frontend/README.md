# Neuro-Learn — Frontend

React 19 + Vite 7 + Tailwind 3 client for Neuro-Learn. It talks only to the Spring Boot backend in `../backend-java`; there are no client-side AI calls.

## Run

```bash
npm install
npm run dev        # http://localhost:5173
```

The backend must be running on `http://localhost:8080` (see the root README's Quick start).

| Variable | Default | Purpose |
|---|---|---|
| `VITE_API_BASE` | `http://localhost:8080/api` | Backend API base URL |

## Scripts

| Command | What it does |
|---|---|
| `npm run dev` | Dev server with hot reload |
| `npm run build` | Production build into `dist/` |
| `npm run preview` | Serve the production build locally |
| `npm run lint` | ESLint |

## Layout

```
src/
  App.jsx              page switcher (home → roadmap → learn → test → feedback → boss, dashboard, auth)
  pages/               one file per screen
  components/          navbar, footer, cursor, character reaction popup
  context/AuthContext  JWT token + user, stored in localStorage
  data/chapters.js     chapter/section list and character art
  utils/api.js         all backend calls + themed story/feedback text
  utils/soundEffects.js  Web Audio sound effects
```

## Demo accounts

Seeded by Flyway `V5`. Password for all of them: `password123`. The Auth page also has 1-click demo buttons.

| Role | Email |
|---|---|
| Student | `bruce@neurolearn.com` |
| Teacher | `teacher@neurolearn.com` |
| Parent | `parent@neurolearn.com` |
