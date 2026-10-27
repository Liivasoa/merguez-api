package mg.msys.merguez_api.infrastructure.group;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import mg.msys.merguez_api.domain.group.Group;
import mg.msys.merguez_api.domain.group.GroupId;
import mg.msys.merguez_api.domain.group.GroupMember;
import mg.msys.merguez_api.domain.group.Role;
import mg.msys.merguez_api.domain.user.UserId;

@Document("groups")
record GroupDocument(@Id String id, String name, String description, List<MemberDocument> members) {

    static GroupDocument from(Group group) {
        List<MemberDocument> members = group.members().stream()
                .map(MemberDocument::from)
                .toList();
        return new GroupDocument(group.id().value().toString(), group.name(), group.description().orElse(null), members);
    }

    Group toGroup() {
        List<GroupMember> groupMembers = members.stream().map(MemberDocument::toGroupMember).toList();
        UserId creator = userIdsWithRole(groupMembers, Role.ADMIN).findFirst().orElseThrow();
        List<UserId> participants = userIdsWithRole(groupMembers, Role.MEMBER).toList();
        return Group.restore(new GroupId(UUID.fromString(id)), name, description, creator, participants);
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
}
