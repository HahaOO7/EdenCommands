package at.haha007.edencommands.tree;

import at.haha007.edencommands.EdenCommandsTestBase;
import at.haha007.edencommands.argument.Completion;
import at.haha007.edencommands.argument.DoubleArgument;
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.google.common.util.concurrent.AtomicDouble;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class CommandNodeTest extends EdenCommandsTestBase {
    private final CommandSender sender = Mockito.mock(CommandSender.class);

    @Test
    void tabLiteral() {
        LiteralCommandNode node = LiteralCommandNode.builder("test").then(LiteralCommandNode.builder("arg")).build();
        commandRegistry().register(node);
        List<AsyncTabCompleteEvent.Completion> completes = simulateCompletion(sender, "/test ");
        Assertions.assertEquals(1, completes.size());
        Assertions.assertEquals("arg", completes.get(0).suggestion());
    }

    @Test
    void executeLiteral() {
        AtomicBoolean executed = new AtomicBoolean(false);
        LiteralCommandNode node = LiteralCommandNode.builder("test")
                .then(LiteralCommandNode.builder("arg").executor(context -> executed.set(true))).build();
        commandRegistry().register(node);
        simulateExecute(sender, "test arg");
        Assertions.assertTrue(executed.get());
    }

    @Test
    void tabArg() {
        DoubleArgument argument = DoubleArgument.builder()
                .notDoubleMessage(s -> Component.text("Argument must be of type double"))
                .completion(new Completion<>(.1, Component.text("meow")))
                .completion(new Completion<>(1.))
                .completion(new Completion<>(10.01))
                .completion(new Completion<>(0.1 + 0.2))
                .filter(new DoubleArgument.MinimumFilter(Component.text("text"), 0))
                .filter(new DoubleArgument.MaximumFilter(Component.text("text"), 10))
                .build();

        LiteralCommandNode node = LiteralCommandNode.builder("test").then(ArgumentCommandNode.builder("arg", argument)).build();
        commandRegistry().register(node);
        List<AsyncTabCompleteEvent.Completion> completes = simulateCompletion(sender, "/test ");
        Assertions.assertEquals(3, completes.size());
        Assertions.assertEquals("0.1", completes.get(0).suggestion());
    }

    @Test
    void executeArg(){
        DoubleArgument argument = DoubleArgument.builder()
                .notDoubleMessage(s -> Component.text("Argument must be of type double"))
                .completion(new Completion<>(.1, Component.text("meow")))
                .completion(new Completion<>(1.))
                .completion(new Completion<>(10.01))
                .completion(new Completion<>(0.1 + 0.2))
                .filter(new DoubleArgument.MinimumFilter(Component.text("text"), 0))
                .filter(new DoubleArgument.MaximumFilter(Component.text("text"), 10))
                .build();
        AtomicDouble atomicDouble = new AtomicDouble();
        LiteralCommandNode node = LiteralCommandNode.builder("test")
                .then(ArgumentCommandNode.builder("arg", argument).executor(context -> {
                    atomicDouble.set(context.parameter("arg"));
                })).build();
        commandRegistry().register(node);
        simulateExecute(sender, "test 0.1");
        Assertions.assertEquals(0.1, atomicDouble.get());
        simulateExecute(sender, "test 1");
        Assertions.assertEquals(1, atomicDouble.get());
        simulateExecute(sender, "test 10");
        Assertions.assertEquals(10, atomicDouble.get());
    }
}
