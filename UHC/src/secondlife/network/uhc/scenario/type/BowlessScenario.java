package secondlife.network.uhc.scenario.type;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import secondlife.network.uhc.scenario.Scenario;
import secondlife.network.vituz.utilties.Color;

public class BowlessScenario extends Scenario implements Listener {

	public BowlessScenario() {
		super("Bowless", Material.BOW, "Bows can't be crafted/used.");
	}

	public static void handleCraft(Player player, Recipe recipe, CraftingInventory inventory, CraftItemEvent event) {
		if(recipe.getResult().getType() != Material.BOW) return;

		inventory.setResult(new ItemStack(Material.AIR));
		player.sendMessage(Color.translate("&cYou can't craft bows while &lBowless&c scenario is active."));
		event.setCancelled(true);
	}

	public static void handleInteract(Player player, ItemStack item) {
		if(item == null) return;
		if(item.getType() != Material.BOW) return;

		player.setItemInHand(null);
		player.updateInventory();

		player.sendMessage(Color.translate("&cYou can't use bow while &lBowless&c scenario is active."));
	}

	@EventHandler
	public void onCraftItem(CraftItemEvent event) {
		Player player = (Player) event.getView().getPlayer();

		if(event.getRecipe().getResult().getType() != Material.BOW) return;
		
		event.getInventory().setResult(new ItemStack(Material.AIR));

		player.sendMessage(Color.translate("&cYou can't craft bows while &lBowless&c scenario is active."));

		event.setCancelled(true);
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		ItemStack item = event.getItem();

		if(item == null) return;
		
		if(item.getType() != Material.BOW) return;
		
		player.setItemInHand(null);
		player.updateInventory();

		player.sendMessage(Color.translate("&cYou can't use bow while &lBowless&c scenario is active."));
	}

}
