package secondlife.network.vituz.punishments.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import secondlife.network.vituz.Vituz;
import secondlife.network.vituz.VituzAPI;
import secondlife.network.vituz.commands.BaseCommand;
import secondlife.network.vituz.data.PunishData;
import secondlife.network.vituz.punishments.redis.PunishPublisher;
import secondlife.network.vituz.punishments.PunishmentQueue;
import secondlife.network.vituz.punishments.PunishmentType;
import secondlife.network.vituz.utilties.Color;
import secondlife.network.vituz.utilties.Permission;

public class BlacklistCommand extends BaseCommand {
	
    public BlacklistCommand(Vituz plugin) {
		super(plugin);
		
		this.command = "blacklist";
		this.permission = Permission.OP_PERMISSION;
	}
    
	@Override
	public void execute(CommandSender sender, String[] args) {       
        String senderName = sender.getName();
        
        if(sender instanceof Player) {
			senderName = sender.getName();
		}

        if(args.length < 2) {
            sender.sendMessage(Color.translate("&cUsage: /blacklist <player> <reason>"));
            sender.sendMessage(Color.translate("&cExample: /blacklist ItsNature Dox"));
            return;
        }

        PunishData profile = PunishData.getByName(args[0]);

        if(!profile.isLoaded()) {
            profile.load();
        }

        // REASON
        StringBuilder sb = new StringBuilder();

        for(int i = 1; i < args.length; ++i) {
            sb.append(args[i]).append(" ");
        }

        String reason = sb.toString().trim();

        boolean silent = false;

        if(reason.contains("-silent")) {
            silent = true;
            reason = reason.replace("-silent", "");
        } else if(reason.contains("-s")) {
            silent = true;
            reason = reason.replace("-s", "");
        }

        if(!VituzAPI.canPunish(sender, args[0])) {
            sender.sendMessage(Color.translate("&cSorry but you can't punish " + args[0] + "!"));
            return;
        }

        if(profile.isBanned()) {
            sender.sendMessage(Color.translate("&c" + ChatColor.stripColor(profile.getName()) + " &cis already banned."));
            return;
        }

        if(profile.isIPBanned()) {
            sender.sendMessage(Color.translate("&c" + ChatColor.stripColor(profile.getName()) + " is ip-banned."));
            return;
        }
        
        new PunishmentQueue(profile.getName(), PunishmentType.BLACKLIST);
        PunishPublisher.write("punishment;BLACKLIST;" + VituzAPI.getServerName() + ";" + profile.getName() + ";" + senderName + ";" + reason + ";" + silent + ";" + VituzAPI.getServerName());
    }
}
