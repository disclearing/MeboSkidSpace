package secondlife.network.hcfactions.commands.arguments;

import org.bukkit.command.CommandSender;

import secondlife.network.hcfactions.HCF;
import secondlife.network.hcfactions.commands.BaseCommand;
import secondlife.network.hcfactions.utilties.file.ConfigFile;
import secondlife.network.vituz.utilties.Color;

public class CoordsCommand extends BaseCommand {

	public CoordsCommand(HCF plugin) {
		super(plugin);
		
		this.command = "coords";
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		for(String msg : ConfigFile.getStringList("coords")) {
			sender.sendMessage(Color.translate(msg));
		}
	}
}
