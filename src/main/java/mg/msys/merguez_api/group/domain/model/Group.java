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
import mg.msys.merguez_api.group.domain.exception.InvalidGroupException;
import mg.msys.merguez_api.user.domain.model.UserId;

public final class Group {

    private final GroupId id;
    private final String name;
    private final String description;
    private final UserId creator;
    private final List<UserId> participants;
    private final List<Session> sessions = new ArrayList<>();
    private final List<SessionOccurrence> activeOccurrences = new ArrayList<>();

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
                throw new IllegalArgumentException("Scheduled session must belong to this group");
            }
            group.validateSessionSchedule(session.day(), session.startTime());
            group.sessions.add(session);
        }
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
            throw new IllegalArgumentException("Session day is required");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("Session start time is required");
        }
        if (sessions.stream().anyMatch(session -> session.day().equals(day) && session.startTime().equals(startTime))) {
            throw new IllegalStateException("A session already exists at this day and start time");
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
            throw new IllegalArgumentException("Scheduled session must belong to this group");
        }
        if (!activeOccurrences.isEmpty()) {
            throw new IllegalStateException("A session is already started in this group");
        }
        SessionOccurrence occurrence = new SessionOccurrence(id, scheduledSession.id(), Instant.now());
        activeOccurrences.add(occurrence);
        return occurrence;
    }

    public List<SessionOccurrence> activeOccurrences() {
        return List.copyOf(activeOccurrences);
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
