package at.haha007.edencommands.tree.node;

import at.haha007.edencommands.tree.CommandContext;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

public class NodeCommand extends Command {
    private final LiteralCommandNode root;

    public NodeCommand(LiteralCommandNode root) {
        super(root.getLiteral());
        this.root = root;
        Bukkit.getCommandMap().register("EdenCommands", this);
    }

    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        CommandContext context = new CommandContext();
        context.setSender(sender);
        Stack<String> stack = new Stack<>();
        Arrays.stream(args).sorted(Comparator.reverseOrder()).forEach(stack::push);
        stack.push(root.getLiteral());
        root.execute(stack, context);
        return context.isWasExecuted();
    }

    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String lab, @NotNull String[] args) throws IllegalArgumentException {
        CommandContext context = new CommandContext();
        context.setSender(sender);
        Stack<String> stack = new Stack<>();
        Arrays.stream(args).sorted(Comparator.reverseOrder()).forEach(stack::push);
        stack.push(root.getLiteral());
        return root.tabComplete(stack, context);
    }
}