package mg.msys.merguez_api.domain.group;

import java.util.List;

import mg.msys.merguez_api.domain.user.User;

public record Group(String name, String description) {

    public static Group create(String name, User creator, List<User> participants) {
        return create(name, null, creator, participants);
    }

    public static Group create(String name, String description, User creator, List<User> participants) {
        return new Group(name, description);
    }
}
