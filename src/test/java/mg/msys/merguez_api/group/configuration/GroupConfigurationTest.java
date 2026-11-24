package mg.msys.merguez_api.group.configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

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
import mg.msys.merguez_api.group.application.port.in.ListMyGroupsUseCase;
import mg.msys.merguez_api.group.application.port.in.StartSessionUseCase;

class GroupConfigurationTest {

    @ParameterizedTest
    @ValueSource(classes = {
            AddRoundUseCase.class,
            AddCombatUseCase.class,
            CloseRoundUseCase.class,
            CreateSessionUseCase.class,
            GetGroupSessionsUseCase.class,
            ListMyGroupsUseCase.class,
            CreateGroupUseCase.class,
            StartSessionUseCase.class,
            CloseSessionUseCase.class
    })
    void registersGroupUseCasesAsSpringBeans(Class<?> useCaseType) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(GroupRepository.class, () -> mock(GroupRepository.class));
            context.registerBean(LoadGroupWithActiveOccurrencePort.class,
                    () -> mock(LoadGroupWithActiveOccurrencePort.class));
            context.registerBean(RoundRepository.class, () -> mock(RoundRepository.class));
            context.registerBean(SessionOccurrenceRepository.class, () -> mock(SessionOccurrenceRepository.class));
            context.registerBean(SaveClosedSessionOccurrencePort.class,
                    () -> mock(SaveClosedSessionOccurrencePort.class));
            context.registerBean(LogSessionClosurePort.class, () -> mock(LogSessionClosurePort.class));
            context.register(GroupConfiguration.class);
            context.refresh();

            assertNotNull(context.getBean(useCaseType));
        }
    }
}
