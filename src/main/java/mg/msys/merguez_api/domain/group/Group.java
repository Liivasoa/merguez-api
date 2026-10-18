package mg.msys.merguez_api.domain.group;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import mg.msys.merguez_api.domain.user.User;

public final class Group {

    private final String name;
    private final String description;

    private Group(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public static Group create(String name, User creator, List<User> participants) {
        return create(name, null, creator, participants);
    }

    public static Group create(String name, String description, User creator, List<User> participants) {
        if (name == null) {
            throw new IllegalArgumentException("Group name is required");
        }
        return new Group(name, description);
    }

    public String name() {
        return name;
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Group group
                && Objects.equals(name, group.name)
                && Objects.equals(description, group.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }

    @Override
    public String toString() {
        return "Group[name=" + name + ", description=" + description + "]";
    }
}
