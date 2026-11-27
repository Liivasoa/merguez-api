# Merguez Frontend

Ionic Angular application for the Merguez group training app. The project uses Angular standalone components and Capacitor for native Android and iOS targets.

## Requirements

- Node.js 22.22.3 (`.nvmrc`) or another version supported by the installed Angular release
- npm

## Start the web app

```powershell
npm ci
npm start
```

## Run in Docker Compose

From the repository root, build and start the frontend, API, and MongoDB together:

```powershell
docker compose up --build -d
```

Open <http://localhost:4200>. The Nginx container serves the production Angular build and forwards `/api/` requests to the `api` Compose service. Stop the stack with `docker compose down`.

## Available checks

```powershell
npm run build
npm run lint
npm run test:unit
npm run test:e2e
```

## Native platforms

Capacitor is configured with `webDir: "www"`. Install the platform tooling, then add the platform you intend to build:

```powershell
npx cap add android
npm run build
npx cap sync android
```

iOS builds require macOS and Xcode. Android builds require Android Studio and the Android SDK. See the [Capacitor environment setup](https://capacitorjs.com/docs/getting-started/environment-setup) for platform prerequisites.

The API base URL and typed client services will be added with the first UI feature that consumes the backend; do not put machine-specific URLs in components.
