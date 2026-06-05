package olympus.hephaestus.org.role.dto;

import java.util.List;

public record UpdateRolePeopleRequest(
        List<Long> personIds
) {
}
