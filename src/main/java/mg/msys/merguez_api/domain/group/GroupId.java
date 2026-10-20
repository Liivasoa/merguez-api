package mg.msys.merguez_api.domain.group;

import java.util.UUID;

public record GroupId(UUID value) {

    public static GroupId random() {
        return new GroupId(UUID.randomUUID());
    }
}
