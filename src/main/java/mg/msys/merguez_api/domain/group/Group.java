package mg.msys.merguez_api.domain.group;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import mg.msys.merguez_api.domain.user.UserId;

public final class Group {

    private final GroupId id;
    private final String name;
    private final String description;
    private final UserId creator;
    private final List<UserId> participants;

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
        return new Group(id, name, description, creator, participants);
    }

    private static void requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Group name is required");
        }
    }

    private static void requireAtLeastOneParticipant(UserId creator, List<UserId> participants) {
        if (participants.stream().allMatch(participant -> participant.equals(creator))) {
            throw new IllegalArgumentException("At least one participant other than the creator is required");
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
