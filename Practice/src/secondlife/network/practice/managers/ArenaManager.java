package secondlife.network.practice.managers;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import secondlife.network.practice.Practice;
import secondlife.network.practice.arena.Arena;
import secondlife.network.practice.arena.StandaloneArena;
import secondlife.network.practice.kit.Kit;
import secondlife.network.practice.utilties.CustomLocation;
import secondlife.network.practice.utilties.file.ArenaFile;
import secondlife.network.practice.utilties.inventory.InventoryUI;
import secondlife.network.practice.utilties.inventory.ItemUtil;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ArenaManager {
	private final Practice plugin = Practice.getInstance();

	@Getter
	private final Map<String, Arena> arenas = new HashMap<>();

	@Getter
	private final Map<StandaloneArena, UUID> arenaMatchUUIDs = new HashMap<>();

	@Getter
	@Setter
	private int generatingArenaRunnables;

	public ArenaManager() {
		this.loadArenas();
	}

	private void loadArenas() {
		FileConfiguration fileConfig = ArenaFile.configuration;
		ConfigurationSection arenaSection = fileConfig.getConfigurationSection("arenas");

		if (arenaSection == null) {
			return;
		}

		arenaSection.getKeys(false).forEach(name -> {
			String a = arenaSection.getString(name + ".a");
			String b = arenaSection.getString(name + ".b");
			String min = arenaSection.getString(name + ".min");
			String max = arenaSection.getString(name + ".max");

			Block aB = (Block) arenaSection.get(name + ".aBed");
			Block bB = (Block) arenaSection.get(name + ".bBed");

			CustomLocation locA = CustomLocation.stringToLocation(a);
			CustomLocation locB = CustomLocation.stringToLocation(b);
			CustomLocation locMin = CustomLocation.stringToLocation(min);
			CustomLocation locMax = CustomLocation.stringToLocation(max);

			List<StandaloneArena> standaloneArenas = new ArrayList<>();

			ConfigurationSection saSection = arenaSection.getConfigurationSection(name + ".standaloneArenas");

			if (saSection != null) {
				saSection.getKeys(false).forEach(id -> {
					String saA = saSection.getString(id + ".a");
					String saB = saSection.getString(id + ".b");
					String saMin = saSection.getString(id + ".min");
					String saMax = saSection.getString(id + ".max");
					//String saAB = saSection.getString(id + ".aBed");
					//String saBB = saSection.getString(id + ".bBed");

					CustomLocation locSaA = CustomLocation.stringToLocation(saA);
					CustomLocation locSaB = CustomLocation.stringToLocation(saB);
					CustomLocation locSaMin = CustomLocation.stringToLocation(saMin);
					CustomLocation locSaMax = CustomLocation.stringToLocation(saMax);
					//CustomLocation locSaAB = CustomLocation.stringToLocation(saAB);
					//CustomLocation locSaBB = CustomLocation.stringToLocation(saBB);

					//boolean bedwars = saSection.getBoolean(name + ".bedwars", false);

					//if(bedwars) {
					//	standaloneArenas.add(new StandaloneArena(locSaA, locSaB, locSaMin, locSaMax, locSaAB, locSaBB));
					//} else {
						standaloneArenas.add(new StandaloneArena(locSaA, locSaB, locSaMin, locSaMax));
					//}
				});
			}

			boolean enabled = arenaSection.getBoolean(name + ".enabled", false);
			boolean bedwars = arenaSection.getBoolean(name + ".bedwars", false);

			Block locBedA = aB;
			Block locBedB = bB;

			Arena arena = new Arena(name, standaloneArenas, new ArrayList<>(standaloneArenas), locA, locB, locMin, locMax, enabled, false, null, null);

			this.arenas.put(name, arena);
		});
	}

	public void saveArenas() {
		FileConfiguration fileConfig = ArenaFile.configuration;

		fileConfig.set("arenas", null);
		arenas.forEach((arenaName, arena) -> {
			String a = CustomLocation.locationToString(arena.getA());
			String b = CustomLocation.locationToString(arena.getB());
			String min = CustomLocation.locationToString(arena.getMin());
			String max = CustomLocation.locationToString(arena.getMax());

			String arenaRoot = "arenas." + arenaName;

			fileConfig.set(arenaRoot + ".a", a);
			fileConfig.set(arenaRoot + ".b", b);
			fileConfig.set(arenaRoot + ".min", min);
			fileConfig.set(arenaRoot + ".max", max);
			fileConfig.set(arenaRoot + ".enabled", arena.isEnabled());
			//fileConfig.set(arenaRoot + ".bedwars", arena.isBedWars());
			//fileConfig.set(arenaRoot + ".aBed", arena.getABed());
			//fileConfig.set(arenaRoot + ".bBed", arena.getBBed());
			fileConfig.set(arenaRoot + ".standaloneArenas", null);
			int i = 0;
			if (arena.getStandaloneArenas() != null) {
				for (StandaloneArena saArena : arena.getStandaloneArenas()) {
					String saA = CustomLocation.locationToString(saArena.getA());
					String saB = CustomLocation.locationToString(saArena.getB());
					String saMin = CustomLocation.locationToString(saArena.getMin());
					String saMax = CustomLocation.locationToString(saArena.getMax());
					//String saAB = CustomLocation.locationToString(saArena.getABed());
					//String saBB = CustomLocation.locationToString(saArena.getBBed());

					String standAloneRoot = arenaRoot + ".standaloneArenas." + i;

					fileConfig.set(standAloneRoot + ".a", saA);
					fileConfig.set(standAloneRoot + ".b", saB);
					fileConfig.set(standAloneRoot + ".min", saMin);
					fileConfig.set(standAloneRoot + ".max", saMax);
					//fileConfig.set(standAloneRoot + ".aBed", saAB);
					//fileConfig.set(standAloneRoot + ".bBed", saBB);

					i++;
				}
			}
		});

		ArenaFile.save();
	}

	public void createArena(String name) {
		this.arenas.put(name, new Arena(name));
	}

	public void deleteArena(String name) {
		this.arenas.remove(name);
	}

	public Arena getArena(String name) {
		return this.arenas.get(name);
	}

	public void reloadArenas() {
		this.saveArenas();
		this.arenas.clear();
		this.loadArenas();
	}

	public void openArenaSystemUI(Player player) {

		if(this.arenas.size() == 0) {
			player.sendMessage(ChatColor.RED + "There's no arenas.");
			return;
		}

		InventoryUI inventory = new InventoryUI("Arena System", true, 6);

		for(Arena arena : this.arenas.values()) {

			ItemStack item = ItemUtil.createItem(Material.PAPER, ChatColor.YELLOW + arena.getName() + ChatColor.GRAY + " (" + (arena.isEnabled() ? ChatColor.GREEN.toString() + ChatColor.BOLD + "ENABLED" : ChatColor.RED.toString() + ChatColor.BOLD + "DISABLED") + ChatColor.GRAY + ")");
			ItemUtil.reloreItem(item, ChatColor.GRAY + "Arenas: " + ChatColor.GREEN + (arena.getStandaloneArenas().size() == 0 ? "Single Arena (Invisible Players)" : arena.getStandaloneArenas().size() + " Arenas"), ChatColor.GRAY + "Standalone Arenas: " + ChatColor.GREEN + (arena.getAvailableArenas().size() == 0 ? "None" : arena.getAvailableArenas().size() + " Arenas Available"), "", ChatColor.YELLOW.toString() + ChatColor.BOLD + "LEFT CLICK " + ChatColor.GRAY + "Teleport to Arena", ChatColor.YELLOW.toString() + ChatColor.BOLD + "RIGHT CLICK " + ChatColor.GRAY + "Generate Standalone Arenas");
			inventory.addItem(new InventoryUI.AbstractClickableItem(item) {
				@Override
				public void onClick(InventoryClickEvent event) {
					Player player = (Player) event.getWhoClicked();

					if(event.getClick() == ClickType.LEFT) {
						player.teleport(arena.getA().toBukkitLocation());
					} else {

						InventoryUI generateInventory = new InventoryUI("Generate Arenas", true, 1);

						int[] batches = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150};

						for(int batch : batches) {
							ItemStack item = ItemUtil.createItem(Material.PAPER, ChatColor.RED.toString() + ChatColor.BOLD + batch + " ARENAS");
							generateInventory.addItem(new InventoryUI.AbstractClickableItem(item) {
								@Override
								public void onClick(InventoryClickEvent event) {
									Player player = (Player) event.getWhoClicked();
									player.performCommand("arena generate " + arena.getName() + " " + batch);
									player.sendMessage(ChatColor.GREEN + "Generating " + batch + " arenas, please check console for progress.");
									player.closeInventory();
								}
							});
						}

						player.openInventory(generateInventory.getCurrentPage());
					}
				}
			});
		}

		player.openInventory(inventory.getCurrentPage());
	}

	public Arena getRandomArena(Kit kit) {
		List<Arena> enabledArenas = new ArrayList<>();

		for (Arena arena : this.arenas.values()) {
			if (!arena.isEnabled()) {
				continue;
			}

			if (kit.getExcludedArenas().contains(arena.getName())) {
				continue;
			}

			if (kit.getArenaWhiteList().size() > 0 && !kit.getArenaWhiteList().contains(arena.getName())) {
				continue;
			}

			enabledArenas.add(arena);
		}

		if (enabledArenas.size() == 0) {
			return null;
		}

		return enabledArenas.get(ThreadLocalRandom.current().nextInt(enabledArenas.size()));
	}

	public void removeArenaMatchUUID(StandaloneArena arena) {
		this.arenaMatchUUIDs.remove(arena);
	}

	public UUID getArenaMatchUUID(StandaloneArena arena) {
		return this.arenaMatchUUIDs.get(arena);
	}

	public void setArenaMatchUUID(StandaloneArena arena, UUID matchUUID) {
		this.arenaMatchUUIDs.put(arena, matchUUID);
	}
}
