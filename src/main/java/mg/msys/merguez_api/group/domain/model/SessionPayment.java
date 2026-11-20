package mg.msys.merguez_api.group.domain.model;

import mg.msys.merguez_api.user.domain.model.UserId;

public record SessionPayment(UserId debtor, UserId creditor, int amount) {
}
