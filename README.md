# Merguez

Merguez helps a group of friends track Brazilian jiu-jitsu rounds and settle session scores. This Spring Boot REST API stores groups, scheduled sessions, session occurrences, rounds, and combat results in MongoDB.

## Features

- Create a group with an administrator and members.
- Schedule recurring sessions by day and start time.
- Start one session occurrence at a time per group.
- Create numbered rounds and record two-person combats.
- Close rounds with a winner or a draw for each combat.
- Close a completed session to calculate each member's net score and suggested payments.

See [the feature overview](docs/features.md) for the current business rules.

## Repository layout

- `backend/` contains the Spring Boot API, Maven build, Docker image, and Postman collections.
- `frontend/` contains the Ionic Angular application and its instructions.
- `.agents/skills/` contains repository skills shared according to their descriptions. Backend and frontend-specific instructions live in each application's `AGENTS.md`.

## Requirements

- Docker with the Compose plugin, to run the API and MongoDB containers
- Maven Wrapper (included under `backend/`)
- Java 25, when running the API outside Docker
- Node.js 22.22.3 or newer in the supported Angular range, when developing the frontend

## Run locally

Build and start the API and MongoDB containers from the project root:

```powershell
docker compose up --build -d
```

The frontend listens on `http://localhost:4200` and the API on `http://localhost:8080`. The frontend container serves the Angular build with Nginx and forwards `/api/` requests to the API container. Compose waits for MongoDB to become healthy before starting the API. To stop the services:

```powershell
docker compose down
```

For local Java development, start only MongoDB and run the API with the Maven Wrapper:

```powershell
docker compose up -d mongodb
$env:SPRING_DOCKER_COMPOSE_ENABLED = 'false'
.\backend\mvnw.cmd -f backend\pom.xml spring-boot:run
```

The environment setting prevents Spring Boot's development-time Compose integration from managing the root Compose file while the API runs as a local process.

The local process connects to `mongodb://localhost:27017/merguez` by default. Set `MONGODB_URI` to use another MongoDB connection string.

To stop MongoDB:

```powershell
docker compose down
```

## API

All routes use the `/api/v1` prefix. Group creation and session scheduling are available through `POST /groups` and `POST /groups/{groupId}/sessions`. List a group's scheduled sessions with `GET /groups/{groupId}/sessions`.

Start a scheduled session:

```http
POST /api/v1/groups/{groupId}/sessions/{sessionId}/start
Content-Type: application/json
```

```json
{
  "requesterId": "11111111-1111-1111-1111-111111111111"
}
```

Create a round with its initial combats:

```http
POST /api/v1/groups/{groupId}/rounds
Content-Type: application/json
```

```json
{
  "requesterId": "11111111-1111-1111-1111-111111111111",
  "combats": [
    {
      "participantIds": [
        "11111111-1111-1111-1111-111111111111",
        "22222222-2222-2222-2222-222222222222"
      ],
      "winnerId": null
    }
  ]
}
```

A combat has exactly two distinct group members. A member can appear in only one combat per round. `winnerId` is optional and, when supplied, must be one of that combat's participants. Additional combats can be added at `POST /api/v1/groups/{groupId}/rounds/{roundNumber}/combats`.

Close a round by sending its complete combat list and results to `POST /api/v1/groups/{groupId}/rounds/{roundNumber}/close`. Results replace the recorded winners; participant order does not matter.

```json
{
  "requesterId": "11111111-1111-1111-1111-111111111111",
  "combats": [
    {
      "participantIds": [
        "11111111-1111-1111-1111-111111111111",
        "22222222-2222-2222-2222-222222222222"
      ],
      "winnerId": "22222222-2222-2222-2222-222222222222"
    }
  ]
}
```

Close a started session with at least one closed round using `POST /api/v1/groups/{groupId}/sessions/{sessionId}/close` and the same `requesterId` body. The response includes member net scores and the payments that settle them. Session closure is rejected if a round is still open; conflicting concurrent changes are rejected rather than overwritten.

Requests identify the acting member by `requesterId`. Authentication is not currently part of this API.

## Postman collections

Import either collection into Postman:

- `backend/postman/merguez-api.postman_collection.json` for individual requests and error cases.
- `backend/postman/merguez-api-e2e.postman_collection.json` for the full happy path.

Start MongoDB and the API first. Set the collection's `baseUrl` if needed, then run the E2E collection in order with one iteration. Do not override its generated identifiers with environment variables of the same name. The run creates a group and session data that remain in MongoDB; the API currently has no cleanup endpoint.

## Development

Run the frontend from its directory:

```powershell
cd frontend
npm ci
npm start
```

The Ionic app is served at the local URL printed by Angular CLI. See `frontend/README.md` for frontend-specific commands and native-platform setup.

Run the test suite with:

```powershell
.\backend\mvnw.cmd -f backend\pom.xml test
```
