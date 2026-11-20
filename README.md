# Merguez API

Merguez helps a group of friends track Brazilian jiu-jitsu rounds and settle session scores. This Spring Boot REST API stores groups, scheduled sessions, session occurrences, rounds, and combat results in MongoDB.

## Features

- Create a group with an administrator and members.
- Schedule recurring sessions by day and start time.
- Start one session occurrence at a time per group.
- Create numbered rounds and record two-person combats.
- Close rounds with a winner or a draw for each combat.
- Close a completed session to calculate each member's net score and suggested payments.

See [the feature overview](docs/features.md) for the current business rules.

## Requirements

- Java 25
- Docker, for the bundled MongoDB service
- Maven Wrapper (included)

## Run locally

Start MongoDB and the API from the project root:

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run
```

The API listens on `http://localhost:8080`. By default it connects to `mongodb://localhost:27017/merguez`. Set `MONGODB_URI` to use another MongoDB connection string.

To stop the local services:

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

- `postman/merguez-api.postman_collection.json` for individual requests and error cases.
- `postman/merguez-api-e2e.postman_collection.json` for the full happy path.

Start MongoDB and the API first. Set the collection's `baseUrl` if needed, then run the E2E collection in order with one iteration. Do not override its generated identifiers with environment variables of the same name. The run creates a group and session data that remain in MongoDB; the API currently has no cleanup endpoint.

## Development

Run the test suite with:

```powershell
.\mvnw.cmd test
```
