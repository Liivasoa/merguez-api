package mg.msys.merguez_api.group.adapter.in.rest.response;

import mg.msys.merguez_api.group.domain.model.GroupSummary;

public record GroupSummaryResponse(
        String name,
        String description,
        int memberCount,
        int scheduledSessionCount,
        boolean admin) {

    public static GroupSummaryResponse from(GroupSummary summary) {
        return new GroupSummaryResponse(
                summary.name(),
                summary.description().orElse(null),
                summary.memberCount(),
                summary.scheduledSessionCount(),
                summary.admin());
    }
}
