package mg.msys.merguez_api.domain.group;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import mg.msys.merguez_api.domain.user.User;

public final class Group {

    private final String name;
    private final String description;
    private final User creator;

    private Group(String name, String description, User creator) {
        this.name = name;
        this.description = description;
        this.creator = creator;
    }

    public static Group create(String name, User creator, List<User> participants) {
        return create(name, null, creator, participants);
    }

    public static Group create(String name, String description, User creator, List<User> participants) {
        requireName(name);
        requireAtLeastOneParticipant(creator, participants);
        return new Group(name, description, creator);
    }

    private static void requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Group name is required");
        }
    }

    private static void requireAtLeastOneParticipant(User creator, List<User> participants) {
        if (participants.stream().allMatch(participant -> participant.equals(creator))) {
            throw new IllegalArgumentException("At least one participant other than the creator is required");
        }
    }

    public String name() {
        return name;
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    public boolean isAdmin(User user) {
        return creator.equals(user);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Group group
                && Objects.equals(name, group.name)
                && Objects.equals(description, group.description)
                && Objects.equals(creator, group.creator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, creator);
    }

    @Override
    public String toString() {
        return "Group[name=" + name + ", description=" + description + ", creator=" + creator + "]";
    }
}
