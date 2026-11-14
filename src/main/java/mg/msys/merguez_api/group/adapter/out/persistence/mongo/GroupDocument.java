package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.GroupMember;
import mg.msys.merguez_api.group.domain.model.Role;
import mg.msys.merguez_api.user.domain.model.UserId;

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
