package mg.msys.merguez_api.group.domain.exception;

public final class CombatParticipantsNotInGroupException extends DomainValidationException {

    public CombatParticipantsNotInGroupException() {
        super("Combat participants must belong to this group");
    }
}
