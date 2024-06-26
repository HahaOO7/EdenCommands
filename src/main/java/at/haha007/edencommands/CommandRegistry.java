package at.haha007.edencommands;

import at.haha007.edencommands.argument.Argument;
import at.haha007.edencommands.tree.ArgumentCommandNode;
import at.haha007.edencommands.tree.ContextBuilder;
import at.haha007.edencommands.tree.LiteralCommandNode;
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class CommandRegistry implements Listener {

    private final Map<String, NodeCommand> registeredCommands = new HashMap<>();
    private final JavaPlugin plugin;

    /**
     * @param plugin the plugin which to register the commands for.
     *               used to create the plugin-aliased command - "/plugin:command"
     */
    public CommandRegistry(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Unregister all commands and the listener of this class.
     */
    public void destroy() {
        registeredCommands.values().forEach(c -> Bukkit.getCommandMap().getKnownCommands().values().remove(c));
        registeredCommands.clear();
        HandlerList.unregisterAll(this);
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * Register a new Command
     *
     * @param node the @{@link LiteralCommandNode} that should be registered
     */
    public void register(@NotNull LiteralCommandNode node) {
        String literal = node.literal().toLowerCase();
        if (registeredCommands.containsKey(literal)) {
            throw new IllegalArgumentException("Already registered command with the key <%s>".formatted(literal));
        }
        NodeCommand command = new AsyncNodeCommand(node);
        registeredCommands.put(literal.toLowerCase(), command);
        Bukkit.getCommandMap().register(plugin.getName(), command);
    }

    @EventHandler
    private void onTabComplete(AsyncTabCompleteEvent event) {
        String buffer = event.getBuffer();
        if (!buffer.startsWith("/"))
            return;
        if (buffer.toLowerCase().startsWith("/" + plugin.getName().toLowerCase() + ":")) {
            buffer = "/" + buffer.substring(plugin.getName().length() + 2);
        }

        String[] args = buffer.substring(1).split("\\s+");
        if (buffer.endsWith(" ")) {
            String[] a = new String[args.length + 1];
            System.arraycopy(args, 0, a, 0, args.length);
            a[a.length - 1] = "";
            args = a;
        }

        NodeCommand command = registeredCommands.get(args[0].toLowerCase());
        if (command == null) return;
        ContextBuilder context = new ContextBuilder(plugin, event.getSender(), args);
        List<AsyncTabCompleteEvent.Completion> completions = command.tabCompleter().apply(context);
        completions = completions.stream().distinct().sorted(Comparator.comparing(AsyncTabCompleteEvent.Completion::suggestion)).toList();
        event.completions(completions);
    }

    /**
     * Alternative to calling LiteralCommandNode.builder(key)
     *
     * @param key the literal
     * @return a new LiteralCommandBuilder
     */
    public static LiteralCommandNode.LiteralCommandBuilder literal(@NotNull String key) {
        return LiteralCommandNode.builder(key);
    }

    /**
     * Alternative to calling ArgumentCommandNode.builder(key)
     *
     * @param key      the key to access the parsed value from the CommandContext
     * @param argument the argument that should be parsed
     * @return a new ArgumentCommandBuilder
     */
    public static <T> ArgumentCommandNode.ArgumentCommandBuilder<T> argument(@NotNull String key, @NotNull Argument<T> argument) {
        return ArgumentCommandNode.builder(key, argument);
    }

    /**
     * Returns a permission filter to be used with CommandBuilder.requires()
     *
     * @param permission the permission key
     * @return A new @{@link Predicate<CommandSender>}
     */
    public static Predicate<CommandContext> permission(@NotNull String permission) {
        return new PermissionRequirement(permission);
    }

    private record PermissionRequirement(String permission) implements Predicate<CommandContext> {
        public boolean test(CommandContext context) {
            return context.sender().hasPermission(permission);
        }
    }

    private abstract static class NodeCommand extends Command {
        protected final LiteralCommandNode rootNode;

        private NodeCommand(LiteralCommandNode rootNode) {
            super(rootNode.literal().toLowerCase());
            this.rootNode = rootNode;
        }

        @NotNull
        public abstract Function<ContextBuilder, @NotNull List<AsyncTabCompleteEvent.Completion>> tabCompleter();
    }

    private class AsyncNodeCommand extends NodeCommand {
        private AsyncNodeCommand(LiteralCommandNode node) {
            super(node);
        }

        @Override
        @NotNull
        public Function<ContextBuilder, List<AsyncTabCompleteEvent.Completion>> tabCompleter() {
            return rootNode::tabComplete;
        }

        public boolean execute(@NotNull CommandSender sender, @NotNull String label, String[] args) {
            String[] input = new String[args.length + 1];
            input[0] = rootNode.literal();
            System.arraycopy(args, 0, input, 1, args.length);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> rootNode.execute(new ContextBuilder(plugin, sender, input)));
            return true;
        }

        @Override
        public boolean testPermissionSilent(@NotNull CommandSender target) {
            ContextBuilder contextBuilder = new ContextBuilder(plugin, target, new String[]{rootNode.literal()});
            return rootNode.testRequirement(contextBuilder.build());
        }

        @Override
        @NotNull
        public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args)
                throws IllegalArgumentException {
            return List.of();
        }
    }
}
