package mg.msys.merguez_api.domain.group;

import java.util.List;

import mg.msys.merguez_api.domain.user.User;

public record Group(String name) {

    public static Group create(String name, User creator, List<User> participants) {
        return new Group(name);
    }
}
