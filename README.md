# merguez-api

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