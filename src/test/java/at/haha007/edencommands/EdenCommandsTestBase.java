package at.haha007.edencommands;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Logger;

//A commandRegistry fake

public class EdenCommandsTestBase {
    private final CommandRegistry commandRegistry;
    private final CommandMap commandMap;
    protected final JavaPlugin plugin = Mockito.mock(JavaPlugin.class);
    protected final Logger testLogger = Logger.getLogger(getClass().getSimpleName());

    public EdenCommandsTestBase() {
        Server mockServer = Mockito.mock(Server.class);
        PluginManager mockPluginManager = Mockito.mock(PluginManager.class);

        Mockito.when(plugin.getName()).thenReturn("EdenCommands");

        Mockito.when(mockServer.getPluginManager()).thenReturn(mockPluginManager);
        Mockito.when(mockServer.getLogger()).thenReturn(testLogger);
        Mockito.when(mockServer.getVersion()).thenReturn("1.0");
        Mockito.when(mockServer.getMinecraftVersion()).thenReturn("1.21");
        Mockito.when(mockServer.getBukkitVersion()).thenReturn("1.21");

        BukkitScheduler mockScheduler = Mockito.mock(BukkitScheduler.class);
        Mockito.when(mockScheduler.runTaskAsynchronously(Mockito.any(JavaPlugin.class), Mockito.any(Runnable.class)))
                .thenAnswer(i -> {
                    i.getArgument(1, Runnable.class).run();
                    return null;
                });
        Mockito.when(mockServer.getScheduler()).thenReturn(mockScheduler);

        commandMap = new SimpleCommandMap(mockServer);
        Mockito.when(mockServer.getCommandMap()).thenReturn(commandMap);

        try {
            Field serverField = Bukkit.class.getDeclaredField("server");
            serverField.setAccessible(true);
            serverField.set(null, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        Bukkit.setServer(mockServer);
        commandRegistry = new CommandRegistry(plugin);
    }

    public List<AsyncTabCompleteEvent.Completion> simulateCompletion(CommandSender sender, String command) {
        AsyncTabCompleteEvent event = new AsyncTabCompleteEvent(
                sender,
                command,
                true,
                new Location(null, 0, 0, 0)
        );
        try {
            Method complete = CommandRegistry.class.getDeclaredMethod("onTabComplete", AsyncTabCompleteEvent.class);
            complete.setAccessible(true);
            complete.invoke(commandRegistry, event);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return event.completions();
    }

    public void simulateExecute(CommandSender sender, String command) {
        commandMap.dispatch(sender, command);
    }

    public CommandRegistry commandRegistry() {
        return commandRegistry;
    }

}
