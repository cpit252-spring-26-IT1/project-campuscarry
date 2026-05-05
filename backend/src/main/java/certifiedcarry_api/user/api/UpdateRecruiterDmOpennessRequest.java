package certifiedcarry_api.user.api;

import certifiedcarry_api.user.model.RecruiterDmOpenness;
import jakarta.validation.constraints.NotNull;

public record UpdateRecruiterDmOpennessRequest(
    @NotNull(message = "recruiterDmOpenness is required") RecruiterDmOpenness recruiterDmOpenness) {}
