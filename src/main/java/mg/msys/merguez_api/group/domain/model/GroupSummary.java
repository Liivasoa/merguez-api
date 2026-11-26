package mg.msys.merguez_api.group.domain.model;

import java.util.Optional;

public record GroupSummary(
        String name,
        Optional<String> description,
        int memberCount,
        int scheduledSessionCount,
        boolean admin) {}
