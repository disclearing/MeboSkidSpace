package secondlife.network.vituz.commands.arguments.staff.gamemode;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import secondlife.network.vituz.Vituz;
import secondlife.network.vituz.VituzAPI;
import secondlife.network.vituz.commands.BaseCommand;
import secondlife.network.vituz.utilties.Color;
import secondlife.network.vituz.utilties.Msg;
import secondlife.network.vituz.utilties.Permission;

public class SurvivalCommand extends BaseCommand {

	public SurvivalCommand(Vituz plugin) {
		super(plugin);
		
		this.command = "survival";
		this.permission = Permission.ADMIN_PERMISSION;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {	
		if(sender instanceof Player) {
			Player player = (Player) sender;

			if(VituzAPI.getServerName().equals("UHC") && !player.isOp()) {
				player.sendMessage(Color.translate("&cYou can't abuse here :)"));
				return;
			}

			if(args.length == 0) {
				player.setGameMode(GameMode.SURVIVAL);

				player.sendMessage(Color.translate("&eYour gamemode has been updated to &dSurvival&e."));
			} else {
				Player target = Bukkit.getPlayer(args[0]);
				
				if(Msg.checkOffline(sender, args[0])) return;
				
				target.setGameMode(GameMode.SURVIVAL);
				
				Msg.log(Bukkit.getConsoleSender(), "Player " + player.getName() + " set gamemode of " + target.getName() + " to " + target.getGameMode() + ".");
				
				target.sendMessage(Color.translate("&eYour gamemode has been updated to &dSurvival &eby " + player.getDisplayName()+ "&e."));
				sender.sendMessage(Color.translate("&eYou have updated gamemode of " + target.getDisplayName() + " &eto &dSurvival&e."));
			}

			return;
		} 
		
		if(args.length == 0) {
			sender.sendMessage(Color.translate("&cUsage: /gms <player>"));
		} else {
			Player target = Bukkit.getPlayer(args[0]);
			
			if(Msg.checkOffline(sender, args[0])) return;
			
			target.setGameMode(GameMode.SURVIVAL);
			
			Msg.log(Bukkit.getConsoleSender(), "CONSOLE set gamemode of " + target.getName() + " to " + target.getGameMode() + ".");
			
			target.sendMessage(Color.translate("&eYour gamemode has been updated to &dSurvival &eby " + Msg.CONSOLE + "&e."));
			sender.sendMessage(Color.translate("&eYou have updated gamemode of " + target.getDisplayName() + " &eto &dSurvival&e."));
		}
	}
}