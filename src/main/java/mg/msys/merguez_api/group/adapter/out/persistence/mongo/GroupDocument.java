package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.GroupMember;
import mg.msys.merguez_api.group.domain.model.Role;
import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.Session;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;
import mg.msys.merguez_api.user.domain.model.UserId;

@Document("groups")
record GroupDocument(
        @Id String id, String name, String description, List<MemberDocument> members, List<SessionDocument> sessions) {

    static GroupDocument from(Group group) {
        List<MemberDocument> members = group.members().stream()
                .map(MemberDocument::from)
                .toList();
        List<SessionDocument> sessions = group.sessions().stream()
                .map(SessionDocument::from)
                .toList();
        return new GroupDocument(group.id().value().toString(), group.name(), group.description().orElse(null), members, sessions);
    }

    Group toGroup() {
        return toGroup(null, List.of());
    }

    Group toGroup(SessionOccurrence occurrence, List<Round> rounds) {
        List<GroupMember> groupMembers = members.stream().map(MemberDocument::toGroupMember).toList();
        UserId creator = userIdsWithRole(groupMembers, Role.ADMIN).findFirst().orElseThrow();
        List<UserId> participants = userIdsWithRole(groupMembers, Role.MEMBER).toList();
        GroupId groupId = new GroupId(UUID.fromString(id));
        List<SessionDocument> persistedSessions = sessions == null ? List.of() : sessions;
        List<Session> restoredSessions =
                persistedSessions.stream().map(session -> session.toSession(groupId)).toList();
        if (occurrence == null) {
            return Group.restore(groupId, name, description, creator, participants, restoredSessions);
        }
        return Group.restore(groupId, name, description, creator, participants, restoredSessions, occurrence, rounds);
    }

    private static Stream<UserId> userIdsWithRole(List<GroupMember> groupMembers, Role role) {
        return groupMembers.stream()
                .filter(member -> member.role() == role)
                .map(GroupMember::userId);
    }

    record MemberDocument(String userId, String role) {

        static MemberDocument from(GroupMember member) {
            return new MemberDocument(member.userId().value().toString(), member.role().name());
        }

        GroupMember toGroupMember() {
            return new GroupMember(new UserId(UUID.fromString(userId)), Role.valueOf(role));
        }
    }

    record SessionDocument(String id, String name, DayOfWeek day, LocalTime startTime) {

        static SessionDocument from(Session session) {
            return new SessionDocument(session.id().toString(), session.name(), session.day(), session.startTime());
        }

        Session toSession(GroupId groupId) {
            return new Session(groupId, UUID.fromString(id), name, day, startTime);
        }
    }
}
