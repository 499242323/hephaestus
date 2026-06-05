package olympus.hephaestus.login.auth.domain;

import java.io.Serializable;

public record LoginSessionUser(
        Long personId,
        String username,
        String personName,
        Long unitId,
        String loginAt
) implements Serializable {
}
