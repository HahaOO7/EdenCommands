package at.haha007.edencommands.annotations;

import at.haha007.edencommands.CommandContext;
import at.haha007.edencommands.EdenCommandsTestBase;
import at.haha007.edencommands.annotations.annotations.*;
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class LiteralAnnotationsTest extends EdenCommandsTestBase {
    private static final class SimpleTestCommand {
        private static String lastExecuted = "";

        @Command("command")
        @DefaultExecutor
        private void defaultExecutor(CommandContext context) {
            lastExecuted = "default";
        }

        @Command("command")
        private void command(CommandContext context) {
            lastExecuted = "command";
        }

        @Command("command a")
        private void a(CommandContext context) {
            lastExecuted = "command a";
        }

        @Command("command b")
        private void b(CommandContext context) {
            lastExecuted = "command b";
        }

        @Command("command b c")
        private void bc(CommandContext context) {
            lastExecuted = "command b c";
        }

        @Command("command b d")
        private void bd(CommandContext context) {
            lastExecuted = "command b d";
        }
    }

    @Test
    void simpleExecute() {
        AnnotatedCommandLoader loader = new AnnotatedCommandLoader(plugin());
        loader.addAnnotated(new SimpleTestCommand());
        loader.register(commandRegistry());
        CommandSender sender = Mockito.mock(CommandSender.class);
        simulateExecute(sender, "command");
        awaitAsyncCommandExecutions();
        assertEquals("command", SimpleTestCommand.lastExecuted);

        simulateExecute(sender, "command a");
        awaitAsyncCommandExecutions();
        assertEquals("command a", SimpleTestCommand.lastExecuted);

        simulateExecute(sender, "command b c");
        awaitAsyncCommandExecutions();
        assertEquals("command b c", SimpleTestCommand.lastExecuted);

        simulateExecute(sender, "command b d");
        awaitAsyncCommandExecutions();
        assertEquals("command b d", SimpleTestCommand.lastExecuted);

        simulateExecute(sender, "command b e");
        awaitAsyncCommandExecutions();
        assertEquals("default", SimpleTestCommand.lastExecuted);
    }

    @Test
    void testReplacements() {
        AnnotatedCommandLoader loader = new AnnotatedCommandLoader(plugin());
        loader.addAnnotated(new SimpleTestCommand());
        loader.mapLiteral("command", "mycommand");
        loader.mapLiteral("a", "A");
        loader.register(commandRegistry());

        CommandSender sender = Mockito.mock(CommandSender.class);
        SimpleTestCommand.lastExecuted = null;

        simulateExecute(sender, "command");
        awaitAsyncCommandExecutions();
        assertNull(SimpleTestCommand.lastExecuted);

        simulateExecute(sender, "mycommand a");
        awaitAsyncCommandExecutions();
        assertEquals("default", SimpleTestCommand.lastExecuted);

        simulateExecute(sender, "mycommand A");
        awaitAsyncCommandExecutions();
        assertEquals("command a", SimpleTestCommand.lastExecuted);

        simulateExecute(sender, "mycommand b");
        awaitAsyncCommandExecutions();
        assertEquals("command b", SimpleTestCommand.lastExecuted);

        //tab completions
        List<String> completions = simulateCompletion(sender, "mycommand ").stream()
                .map(AsyncTabCompleteEvent.Completion::suggestion).toList();
        assertEquals(2, completions.size());
        assertTrue(completions.contains("A"));
        assertTrue(completions.contains("b"));
        assertFalse(completions.contains("a"));
    }

}