# merguez-api

## Postman

Import `postman/merguez-api.postman_collection.json` for individual requests and
error cases, or `postman/merguez-api-e2e.postman_collection.json` for the
complete happy path. Start MongoDB and the application, set the collection's
`baseUrl` if needed, and run the E2E collection in order with the Postman
Collection Runner (one iteration). Do not override its identifiers with
environment variables of the same name.

The E2E run creates a group with one administrator and five members, schedules
and starts a session, then creates and closes two rounds. Each run creates
and leaves a new group and session data in the configured MongoDB database.
The API currently has no endpoint for deleting this data.

## Close a session

`POST /api/v1/groups/{groupId}/sessions/{sessionId}/close` closes a started
session. Send the ID of the requesting group member:

```json
{
  "requesterId": "11111111-1111-1111-1111-111111111111"
}
```

The response contains the closed occurrence, each member's net score, and the
resulting payments.

## Close a round

`POST /api/v1/groups/{groupId}/rounds/{roundNumber}/close` closes an in-progress
round in the active session. Send the requesting group member and the complete
list of existing combats. Each combat is identified by its two participants;
their order does not matter. `winnerId` must identify a participant, or be
omitted/null when there is no winner. The supplied results replace any previously
recorded winners.

```json
{
  "requesterId": "11111111-1111-1111-1111-111111111111",
  "combats": [
    {
      "participantIds": ["11111111-1111-1111-1111-111111111111", "22222222-2222-2222-2222-222222222222"],
      "winnerId": "22222222-2222-2222-2222-222222222222"
    }
  ]
}
```

The response contains the round with status `CLOSED` and its updated combats.
Concurrent changes are rejected rather than overwritten.