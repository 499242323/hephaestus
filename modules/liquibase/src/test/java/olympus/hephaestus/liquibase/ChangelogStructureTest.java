package olympus.hephaestus.liquibase;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ChangelogStructureTest {

    @Test
    void loadsSingleChangelogFromClasspath() {
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("db/changelog/db.changelog.xml");

        assertThat(stream).isNotNull();
    }
}
