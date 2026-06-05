package olympus.hephaestus;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class ChatStaticAssetsTest {

    @Test
    void chatScriptParsesCleanly() throws Exception {
        ProcessResult nodeVersion = run("node", "--version");
        Assumptions.assumeTrue(nodeVersion.exitCode() == 0,
                () -> "Skipping chat.js syntax validation because Node.js is unavailable: " + nodeVersion.stderr());

        Path tempFile = Files.createTempFile("hephaestus-chat-", ".js");
        try {
            try (InputStream inputStream = getClass().getResourceAsStream("/static/chat.js")) {
                assertNotNull(inputStream, "chat.js resource should exist");
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            ProcessResult result = run("node", "--check", tempFile.toString());
            assertEquals(0, result.exitCode(), () ->
                    "chat.js should parse cleanly."
                            + System.lineSeparator()
                            + "stdout:"
                            + System.lineSeparator()
                            + result.stdout()
                            + System.lineSeparator()
                            + "stderr:"
                            + System.lineSeparator()
                            + result.stderr());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private ProcessResult run(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).start();
        boolean finished = process.waitFor(15, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            fail("Command timed out: " + String.join(" ", command));
        }

        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new ProcessResult(process.exitValue(), stdout, stderr);
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {
    }
}
