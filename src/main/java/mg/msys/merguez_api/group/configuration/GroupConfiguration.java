package mg.msys.merguez_api.group.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.application.port.out.LoadGroupWithActiveOccurrencePort;
import mg.msys.merguez_api.group.application.port.out.LogSessionClosurePort;
import mg.msys.merguez_api.group.application.port.out.RoundRepository;
import mg.msys.merguez_api.group.application.port.out.SaveClosedSessionOccurrencePort;
import mg.msys.merguez_api.group.application.port.out.SessionOccurrenceRepository;
import mg.msys.merguez_api.group.application.port.in.AddCombatUseCase;
import mg.msys.merguez_api.group.application.port.in.AddRoundUseCase;
import mg.msys.merguez_api.group.application.port.in.CloseRoundUseCase;
import mg.msys.merguez_api.group.application.port.in.CloseSessionUseCase;
import mg.msys.merguez_api.group.application.port.in.CreateGroupUseCase;
import mg.msys.merguez_api.group.application.port.in.CreateSessionUseCase;
import mg.msys.merguez_api.group.application.port.in.GetGroupSessionsUseCase;
import mg.msys.merguez_api.group.application.port.in.StartSessionUseCase;
import mg.msys.merguez_api.group.application.usecase.AddCombatService;
import mg.msys.merguez_api.group.application.usecase.AddRoundService;
import mg.msys.merguez_api.group.application.usecase.CloseRoundService;
import mg.msys.merguez_api.group.application.usecase.CloseSessionService;
import mg.msys.merguez_api.group.application.usecase.CreateGroupService;
import mg.msys.merguez_api.group.application.usecase.CreateSessionService;
import mg.msys.merguez_api.group.application.usecase.GetGroupSessionsService;
import mg.msys.merguez_api.group.application.usecase.StartSessionService;

@Configuration
public class GroupConfiguration {

    @Bean
    CloseSessionUseCase closeSessionUseCase(
            LoadGroupWithActiveOccurrencePort groupLoader,
            GroupRepository groupRepository,
            SaveClosedSessionOccurrencePort occurrenceSaver,
            LogSessionClosurePort closureLogger) {
        return new CloseSessionService(groupLoader, groupRepository, occurrenceSaver, closureLogger);
    }

    @Bean
    CloseRoundUseCase closeRoundUseCase(
            LoadGroupWithActiveOccurrencePort groupLoader,
            RoundRepository roundRepository) {
        return new CloseRoundService(groupLoader, roundRepository);
    }

    @Bean
    AddRoundUseCase addRoundUseCase(
            LoadGroupWithActiveOccurrencePort groupLoader,
            RoundRepository roundRepository) {
        return new AddRoundService(groupLoader, roundRepository);
    }

    @Bean
    AddCombatUseCase addCombatUseCase(
            LoadGroupWithActiveOccurrencePort groupLoader,
            RoundRepository roundRepository) {
        return new AddCombatService(groupLoader, roundRepository);
    }

    @Bean
    CreateGroupUseCase createGroupUseCase(GroupRepository groupRepository) {
        return new CreateGroupService(groupRepository);
    }

    @Bean
    CreateSessionUseCase createSessionUseCase(GroupRepository groupRepository) {
        return new CreateSessionService(groupRepository);
    }

    @Bean
    GetGroupSessionsUseCase getGroupSessionsUseCase(GroupRepository groupRepository) {
        return new GetGroupSessionsService(groupRepository);
    }

    @Bean
    StartSessionUseCase startSessionUseCase(
            GroupRepository groupRepository,
            SessionOccurrenceRepository occurrenceRepository) {
        return new StartSessionService(groupRepository, occurrenceRepository);
    }
}
