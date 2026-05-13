# Afya Web (interface)

Application **React + TypeScript + Vite** pour consommer l’API Afya (`/api/v1/`).

## Prérequis

- Node.js 20+ recommandé  
- Backend Spring Boot démarré sur le port **8090** avec JWT et utilisateur bootstrap configurés (voir `.env.example` à la racine du projet Java).

## Démarrage

```bash
cd frontend
npm install
npm run dev
```

Ouvrir [http://localhost:5173](http://localhost:5173). Les requêtes `/api` sont **proxifiées** vers `http://localhost:8090` (voir `vite.config.ts`).

## Production

```bash
npm run build
```

Déployer le dossier `dist/` derrière un serveur statique. Définir **`VITE_API_BASE_URL`** vers l’URL du backend et ajouter cette origine dans **`APP_CORS_ALLOWED_ORIGINS`** côté Spring.
