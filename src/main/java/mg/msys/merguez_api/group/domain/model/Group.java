package mg.msys.merguez_api.group.domain.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import mg.msys.merguez_api.group.domain.exception.GroupAccessDeniedException;
import mg.msys.merguez_api.group.domain.exception.CombatParticipantsNotInGroupException;
import mg.msys.merguez_api.group.domain.exception.DuplicateSessionScheduleException;
import mg.msys.merguez_api.group.domain.exception.InvalidGroupException;
import mg.msys.merguez_api.group.domain.exception.InvalidSessionOccurrenceException;
import mg.msys.merguez_api.group.domain.exception.InvalidSessionScheduleException;
import mg.msys.merguez_api.group.domain.exception.ScheduledSessionDoesNotBelongToGroupException;
import mg.msys.merguez_api.group.domain.exception.SessionAlreadyStartedDomainException;
import mg.msys.merguez_api.group.domain.exception.SessionOccurrenceNotInProgressException;
import mg.msys.merguez_api.group.domain.exception.SessionClosureRejectedException;
import mg.msys.merguez_api.user.domain.model.UserId;

public final class Group {

    private final GroupId id;
    private final String name;
    private final String description;
    private final UserId creator;
    private final List<UserId> participants;
    private final List<Session> sessions = new ArrayList<>();
    private final List<SessionOccurrence> closedOccurrences = new ArrayList<>();
    private SessionOccurrence activeOccurrence;

    private Group(GroupId id, String name, String description, UserId creator, List<UserId> participants) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.creator = creator;
        this.participants = List.copyOf(participants);
    }

    public static Group create(String name, UserId creator, List<UserId> participants) {
        return create(name, null, creator, participants);
    }

    public static Group create(String name, String description, UserId creator, List<UserId> participants) {
        requireName(name);
        requireAtLeastOneParticipant(creator, participants);
        return new Group(GroupId.random(), name, description, creator, participants);
    }

    // For persistence adapters: rebuilds an existing group and bypasses creation validation
    public static Group restore(GroupId id, String name, String description, UserId creator, List<UserId> participants) {
        return restore(id, name, description, creator, participants, List.of());
    }

    public static Group restore(
            GroupId id,
            String name,
            String description,
            UserId creator,
            List<UserId> participants,
            List<Session> sessions) {
        Group group = new Group(id, name, description, creator, participants);
        for (Session session : sessions) {
            if (!id.equals(session.groupId())) {
                throw new ScheduledSessionDoesNotBelongToGroupException();
            }
            group.validateSessionSchedule(session.day(), session.startTime());
            group.sessions.add(session);
        }
        return group;
    }

    public static Group restore(
            GroupId id,
            String name,
            String description,
            UserId creator,
            List<UserId> participants,
            List<Session> sessions,
            SessionOccurrence activeOccurrence,
            List<Round> rounds) {
        Group group = restore(id, name, description, creator, participants, sessions);
        if (activeOccurrence == null || !id.equals(activeOccurrence.groupId())) {
            throw new InvalidSessionOccurrenceException("Active occurrence must belong to this group");
        }
        if (group.sessions.stream().noneMatch(session -> session.id().equals(activeOccurrence.scheduleId()))) {
            throw new InvalidSessionOccurrenceException("Active occurrence must reference a scheduled session");
        }
        group.activeOccurrence = activeOccurrence.attachRounds(rounds);
        return group;
    }

    public Session createSession(UserId requester, String name, DayOfWeek day, LocalTime startTime) {
        if (!isMemberOrAdmin(requester)) {
            throw new GroupAccessDeniedException("Only group members can create sessions");
        }
        validateSessionSchedule(day, startTime);
        Session session = new Session(id, UUID.randomUUID(), sessionNameOrDefault(name, day, startTime), day, startTime);
        sessions.add(session);
        return session;
    }

    private void validateSessionSchedule(DayOfWeek day, LocalTime startTime) {
        if (day == null) {
            throw new InvalidSessionScheduleException("Session day is required");
        }
        if (startTime == null) {
            throw new InvalidSessionScheduleException("Session start time is required");
        }
        if (sessions.stream().anyMatch(session -> session.day().equals(day) && session.startTime().equals(startTime))) {
            throw new DuplicateSessionScheduleException();
        }
    }

    private static String sessionNameOrDefault(String name, DayOfWeek day, LocalTime startTime) {
        if (name == null || name.isEmpty()) {
            return day + " " + startTime;
        }
        return name;
    }

    public SessionOccurrence startSession(UserId requester, Session scheduledSession) {
        if (!isMemberOrAdmin(requester)) {
            throw new GroupAccessDeniedException("Only group members can start sessions");
        }
        if (scheduledSession == null || !sessions.contains(scheduledSession)) {
            throw new ScheduledSessionDoesNotBelongToGroupException();
        }
        if (activeOccurrence != null) {
            throw new SessionAlreadyStartedDomainException();
        }
        activeOccurrence = new SessionOccurrence(id, scheduledSession.id(), Instant.now()).attachRounds(List.of());
        return activeOccurrence;
    }

    public Round addRound(UserId requester, SessionOccurrence occurrence, List<Combat> combats) {
        if (!isMemberOrAdmin(requester)) {
            throw new GroupAccessDeniedException("Only group members can add rounds");
        }
        return requireActiveOccurrence(occurrence).addRound(combats, this::requireGroupParticipants);
    }

    public Round addCombat(UserId requester, Round round, Combat combat) {
        if (!isMemberOrAdmin(requester)) {
            throw new GroupAccessDeniedException("Only group members can add combats");
        }
        return requireActiveOccurrence(round == null ? null : round.occurrence())
                .addCombat(round, combat, this::requireGroupParticipants);
    }

    public Round closeRound(UserId requester, Round round, List<Combat> results) {
        if (!isMemberOrAdmin(requester)) {
            throw new GroupAccessDeniedException("Only group members can close rounds");
        }
        return requireActiveOccurrence(round == null ? null : round.occurrence()).closeRound(round, results);
    }

    public SessionClosureResult closeSession(UserId requester, SessionOccurrence occurrence) {
        if (!isMemberOrAdmin(requester)) {
            throw new GroupAccessDeniedException("Only group members can close sessions");
        }
        if (!isActiveOccurrence(occurrence)) {
            throw new SessionClosureRejectedException("Session occurrence must be in progress");
        }

        List<UserId> memberIds = members().stream().map(GroupMember::userId).toList();
        SessionSettlement settlement = activeOccurrence.settleAndClose(memberIds);
        occurrence.markClosed();
        closedOccurrences.add(activeOccurrence);
        activeOccurrence = null;
        return new SessionClosureResult(occurrence, settlement.netScores(), settlement.payments());
    }

    public List<SessionOccurrence> activeOccurrences() {
        return activeOccurrence == null ? List.of() : List.of(activeOccurrence);
    }

    public List<Round> rounds(SessionOccurrence occurrence) {
        for (SessionOccurrence closedOccurrence : closedOccurrences) {
            if (closedOccurrence == occurrence) {
                return closedOccurrence.rounds();
            }
        }
        if (activeOccurrence != null && activeOccurrence.equals(occurrence)) {
            return activeOccurrence.rounds();
        }
        return closedOccurrences.stream()
                .filter(closedOccurrence -> closedOccurrence.equals(occurrence))
                .findFirst()
                .map(SessionOccurrence::rounds)
                .orElseGet(List::of);
    }

    private SessionOccurrence requireActiveOccurrence(SessionOccurrence occurrence) {
        if (!isActiveOccurrence(occurrence)) {
            throw new SessionOccurrenceNotInProgressException();
        }
        return activeOccurrence;
    }

    private boolean isActiveOccurrence(SessionOccurrence occurrence) {
        return occurrence != null && !occurrence.isClosed()
                && activeOccurrence != null && activeOccurrence.equals(occurrence);
    }

    private void requireGroupParticipants(Combat combat) {
        if (!combat.participants().stream().allMatch(this::isMemberOrAdmin)) {
            throw new CombatParticipantsNotInGroupException();
        }
    }

    private boolean isMemberOrAdmin(UserId requester) {
        return requester != null && (isAdmin(requester) || isMember(requester));
    }

    private static void requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidGroupException("Group name is required");
        }
    }

    private static void requireAtLeastOneParticipant(UserId creator, List<UserId> participants) {
        if (participants.stream().allMatch(participant -> participant.equals(creator))) {
            throw new InvalidGroupException("At least one participant other than the creator is required");
        }
    }

    public GroupId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    public List<Session> sessions() {
        return List.copyOf(sessions);
    }

    public List<GroupMember> members() {
        return Stream.concat(
                        Stream.of(new GroupMember(creator, Role.ADMIN)),
                        participants.stream().map(participant -> new GroupMember(participant, Role.MEMBER)))
                .toList();
    }

    public boolean isAdmin(UserId userId) {
        return creator.equals(userId);
    }

    public boolean isMember(UserId userId) {
        return participants.contains(userId);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Group group
                && Objects.equals(id, group.id)
                && Objects.equals(name, group.name)
                && Objects.equals(description, group.description)
                && Objects.equals(creator, group.creator)
                && Objects.equals(participants, group.participants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, creator, participants);
    }

    @Override
    public String toString() {
        return "Group[id=" + id + ", name=" + name + ", description=" + description + ", creator=" + creator + ", participants=" + participants + "]";
    }
}
