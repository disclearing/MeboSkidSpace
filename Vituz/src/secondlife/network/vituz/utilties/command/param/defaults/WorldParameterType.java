package secondlife.network.vituz.utilties.command.param.defaults;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import secondlife.network.vituz.Vituz;
import secondlife.network.vituz.utilties.command.param.ParameterType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WorldParameterType implements ParameterType<World> {

    public World transform(CommandSender sender, String source) {
        World world = Vituz.getInstance().getServer().getWorld(source);

        if (world == null) {
            sender.sendMessage(ChatColor.RED + "No world with the name " + source + " found.");
            return (null);
        }

        return (world);
    }

    public List<String> tabComplete(Player sender, Set<String> flags, String source) {
        List<String> completions = new ArrayList<>();

        for (World world : Vituz.getInstance().getServer().getWorlds()) {
            if (StringUtils.startsWithIgnoreCase(world.getName(), source)) {
                completions.add(world.getName());
            }
        }

        return (completions);
    }

}