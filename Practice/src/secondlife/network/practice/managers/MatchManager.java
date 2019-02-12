package secondlife.network.practice.managers;

import lombok.Getter;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import secondlife.network.practice.Practice;
import secondlife.network.practice.arena.Arena;
import secondlife.network.practice.events.EventPlayer;
import secondlife.network.practice.events.PracticeEvent;
import secondlife.network.practice.handlers.CustomMovementHandler;
import secondlife.network.practice.inventory.InventorySnapshot;
import secondlife.network.practice.kit.Kit;
import secondlife.network.practice.kit.PlayerKit;
import secondlife.network.practice.match.Match;
import secondlife.network.practice.match.MatchRequest;
import secondlife.network.practice.match.MatchState;
import secondlife.network.practice.match.MatchTeam;
import secondlife.network.practice.player.PracticeData;
import secondlife.network.practice.player.PlayerState;
import secondlife.network.practice.queue.QueueType;
import secondlife.network.practice.runnable.RematchRunnable;
import secondlife.network.practice.utilties.CC;
import secondlife.network.practice.utilties.PlayerUtil;
import secondlife.network.practice.utilties.TtlHashMap;
import secondlife.network.practice.utilties.event.match.MatchEndEvent;
import secondlife.network.practice.utilties.event.match.MatchStartEvent;
import secondlife.network.vituz.VituzAPI;
import secondlife.network.vituz.providers.nametags.VituzNametagHandler;
import secondlife.network.vituz.utilties.ActionMessage;
import secondlife.network.vituz.utilties.Color;
import secondlife.network.vituz.utilties.ItemBuilder;
import secondlife.network.vituz.utilties.Permission;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MatchManager {

	private final Map<UUID, Set<MatchRequest>> matchRequests = new TtlHashMap<>(TimeUnit.SECONDS, 30);
	private final Map<UUID, UUID> rematchUUIDs = new TtlHashMap<>(TimeUnit.SECONDS, 30);
	private final Map<UUID, UUID> rematchInventories = new TtlHashMap<>(TimeUnit.SECONDS, 30);
	private final Map<UUID, UUID> spectators = new ConcurrentHashMap<>();
	@Getter
	private final Map<UUID, Match> matches = new ConcurrentHashMap<>();

	private final Practice plugin = Practice.getInstance();

	public int getFighters() {
		int i = 0;
		for (Match match : this.matches.values()) {
			for (MatchTeam matchTeam : match.getTeams()) {
				i += matchTeam.getAlivePlayers().size();
			}
		}
		return i;
	}

	public int getFighters(String ladder, QueueType type) {
		return (int) this.matches.entrySet().stream().filter(match -> match.getValue().getType() == type)
				.filter(match -> match.getValue().getKit().getName().equals(ladder)).count();
	}

	public void createMatchRequest(Player requester, Player requested, Arena arena, String kitName, boolean party) {
		MatchRequest request = new MatchRequest(requester.getUniqueId(), requested.getUniqueId(), arena, kitName, party);

		this.matchRequests.computeIfAbsent(requested.getUniqueId(), k -> new HashSet<>()).add(request);
	}

	public List<UUID> getOpponents(Match match, Player player) {
		for(MatchTeam team : match.getTeams()) {
			if(team.getPlayers().contains(player.getUniqueId())) {
				continue;
			}

			return team.getPlayers();
		}

		return null;
	}

	public MatchRequest getMatchRequest(UUID requester, UUID requested) {
		Set<MatchRequest> requests = this.matchRequests.get(requested);

		if (requests == null) {
			return null;
		}

		return requests.stream().filter(req -> req.getRequester().equals(requester)).findAny().orElse(null);
	}

	public MatchRequest getMatchRequest(UUID requester, UUID requested, String kitName) {
		Set<MatchRequest> requests = this.matchRequests.get(requested);

		if (requests == null) {
			return null;
		}
		return requests.stream().filter(req -> req.getRequester().equals(requester) && req.getKitName().equals
				(kitName))
				.findAny()
				.orElse(null);
	}

	public Match getMatch(PracticeData playerData) {
		return this.matches.get(playerData.getCurrentMatchID());
	}

	public Match getMatch(UUID uuid) {
		PracticeData playerData = PracticeData.getByName(Bukkit.getPlayer(uuid).getName());
		return this.getMatch(playerData);
	}

	public Match getMatchFromUUID(UUID uuid) {
		return this.matches.containsKey(uuid) ? this.matches.get(uuid) : null;
	}

	public Match getSpectatingMatch(UUID uuid) {
		return this.matches.get(this.spectators.get(uuid));
	}

	public void removeMatchRequests(UUID uuid) {
		this.matchRequests.remove(uuid);
	}

	public void createMatch(Match match) {
		this.matches.put(match.getMatchId(), match);

		this.plugin.getServer().getPluginManager().callEvent(new MatchStartEvent(match));
	}

	public void removeFighter(Player player, PracticeData playerData, boolean spectateDeath) {
		Match match = this.matches.get(playerData.getCurrentMatchID());

		Player killer = player.getKiller();

		MatchTeam entityTeam = match.getTeams().get(playerData.getTeamID());
		MatchTeam winningTeam = match.isFFA() ? entityTeam : match.getTeams().get(entityTeam.getTeamID() == 0 ? 1 : 0);

		if (match.getMatchState() == MatchState.ENDING) {
			return;
		}

		String deathMessage = CC.SECONDARY + player.getName() + CC.PRIMARY + " was " +
				(killer != null ? "slain by " + CC.SECONDARY + killer.getName() + CC.PRIMARY :
						"killed") + "!";

		match.broadcast(deathMessage);

		VituzNametagHandler.reloadPlayer(player);
		VituzNametagHandler.reloadOthersFor(player);

		if(killer != null) {
		    VituzNametagHandler.reloadPlayer(killer);
            VituzNametagHandler.reloadOthersFor(killer);
        }

		if (match.isRedrover()) {
			if (match.getMatchState() != MatchState.SWITCHING) {
				ActionMessage inventories = new ActionMessage();

				inventories.addText(CC.PRIMARY + "Inventories: ");
				if (killer != null) {
					InventorySnapshot snapshot = new InventorySnapshot(killer, match);
					this.plugin.getInventoryManager().addSnapshot(snapshot);
					inventories.addText(CC.GREEN + killer.getName() + " ").addHoverText(Color.translate("&eView Inventory")).setClickEvent(ActionMessage.ClickableType.RunCommand, "/inv " + snapshot.getSnapshotId());
				}
				InventorySnapshot snapshot = new InventorySnapshot(player, match);
				this.plugin.getInventoryManager().addSnapshot(snapshot);
				inventories.addText(CC.RED + player.getName() + " ").addHoverText(
						CC.PRIMARY + "View Inventory").setClickEvent(ActionMessage.ClickableType.RunCommand, "/inv " + snapshot.getSnapshotId());
				match.broadcast(inventories);
				match.setMatchState(MatchState.SWITCHING);
				match.setCountdown(4);
			}
		} else {
			match.addSnapshot(player);
		}

		entityTeam.killPlayer(player.getUniqueId());

		int remaining = entityTeam.getAlivePlayers().size();

		if (remaining != 0) {
			Set<Item> items = new HashSet<>();
			for (ItemStack item : player.getInventory().getContents()) {
				if (item != null && item.getType() != Material.AIR) {
					items.add(player.getWorld().dropItemNaturally(player.getLocation(), item, player));
				}
			}
			for (ItemStack item : player.getInventory().getArmorContents()) {
				if (item != null && item.getType() != Material.AIR) {
					items.add(player.getWorld().dropItemNaturally(player.getLocation(), item, player));
				}
			}
			this.plugin.getMatchManager().addDroppedItems(match, items);
		}

		if (spectateDeath) {
			this.addDeathSpectator(player, playerData, match);
		}

		if ((match.isFFA() && remaining == 1) || remaining == 0) {
			this.plugin.getServer().getPluginManager().callEvent(new MatchEndEvent(match, winningTeam, entityTeam));
		}
	}

	public void removeMatch(Match match) {
		this.matches.remove(match.getMatchId());
		CustomMovementHandler.getParkourCheckpoints().remove(match);
	}

	public void giveKits(Player player, Kit kit) {
		PracticeData playerData = PracticeData.getByName(player.getName());
		Collection<PlayerKit> playerKits = playerData.getPlayerKits(kit.getName()).values();

		if (playerKits.size() == 0) {
			kit.applyToPlayer(player);
		} else {
			player.getInventory().setItem(8, this.plugin.getItemManager().getDefaultBook());
			int slot = -1;
			for (PlayerKit playerKit : playerKits) {
				player.getInventory().setItem(++slot,
						new ItemBuilder(Material.ENCHANTED_BOOK).name(CC.PRIMARY + playerKit.getDisplayName()).build());
			}
			player.updateInventory();
		}
	}

	private void addDeathSpectator(Player player, PracticeData playerData, Match match) {
		this.spectators.put(player.getUniqueId(), match.getMatchId());

		playerData.setPlayerState(PlayerState.SPECTATING);

		PlayerUtil.clearPlayer(player);

		CraftPlayer playerCp = (CraftPlayer) player;
		EntityPlayer playerEp = playerCp.getHandle();

		playerEp.getDataWatcher().watch(6, 0.0F);
		playerEp.setFakingDeath(true);

		match.addSpectator(player.getUniqueId());

		match.addRunnable(this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
			match.getTeams().forEach(team -> team.alivePlayers().forEach(member -> {
				member.hidePlayer(player);
			}));

			match.spectatorPlayers().forEach(member -> member.hidePlayer(player));

			player.getActivePotionEffects().stream().map(PotionEffect::getType).forEach(player::removePotionEffect);
			player.setWalkSpeed(0.2F);
			player.setAllowFlight(true);
		}, 20L));

		if (match.isRedrover()) {
			for (MatchTeam team : match.getTeams()) {
				for (UUID alivePlayerUUID : team.getAlivePlayers()) {
					Player alivePlayer = this.plugin.getServer().getPlayer(alivePlayerUUID);

					if (alivePlayer != null) {
						player.showPlayer(alivePlayer);
					}
				}
			}
		}

		player.setWalkSpeed(0.0F);
		player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 10000, -5));
		//player.setVelocity(player.getLocation().getDirection().setY(1));

		if (match.isParty() || match.isFFA()) {
			this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () ->
					player.getInventory().setContents(this.plugin.getItemManager().getPartySpecItems()), 1L);
		}
		player.updateInventory();
	}

	public void addRedroverSpectator(Player player, Match match) {
		this.spectators.put(player.getUniqueId(), match.getMatchId());

		player.setAllowFlight(true);
		player.setFlying(true);
		player.getInventory().setContents(this.plugin.getItemManager().getPartySpecItems());
		player.updateInventory();

		PracticeData playerData = PracticeData.getByName(player.getName());

		playerData.setPlayerState(PlayerState.SPECTATING);
	}

	public void addSpectator(Player player, PracticeData playerData, Player target, Match targetMatch) {
		this.spectators.put(player.getUniqueId(), targetMatch.getMatchId());

		if (targetMatch.getMatchState() != MatchState.ENDING) {
			if(!targetMatch.haveSpectated(player.getUniqueId())) {
				if(!player.hasPermission(Permission.STAFF_PERMISSION)) {
					targetMatch.broadcast(CC.SECONDARY + player.getName() + CC.PRIMARY + " is now spectating.");
				}
			}
		}

		targetMatch.addSpectator(player.getUniqueId());

		playerData.setPlayerState(PlayerState.SPECTATING);

		player.teleport(target);
		player.setAllowFlight(true);
		player.setFlying(true);

		player.getInventory().setContents(this.plugin.getItemManager().getSpecItems());
		player.updateInventory();

		this.plugin.getServer().getOnlinePlayers().forEach(online -> {
			online.hidePlayer(player);
			player.hidePlayer(online);
		});
		targetMatch.getTeams().forEach(team -> team.alivePlayers().forEach(player::showPlayer));
	}

	public void addDroppedItem(Match match, Item item) {
		match.addEntityToRemove(item);
		match.addRunnable(this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
			match.removeEntityToRemove(item);
			item.remove();
		}, 100L).getTaskId());
	}

	public void addDroppedItems(Match match, Set<Item> items) {
		for (Item item : items) {
			match.addEntityToRemove(item);
		}
		match.addRunnable(this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
			for (Item item : items) {
				match.removeEntityToRemove(item);
				item.remove();
			}
		}, 100L).getTaskId());
	}

	public void removeSpectator(Player player) {
		if(!this.spectators.containsKey(player.getUniqueId())) return;

		Match match = this.matches.get(this.spectators.get(player.getUniqueId()));

		if(match != null) {
			match.removeSpectator(player.getUniqueId());

			PracticeData playerData = PracticeData.getByName(player.getName());
			if (match.getTeams().size() > playerData.getTeamID() && playerData.getTeamID() >= 0) {
				MatchTeam entityTeam = match.getTeams().get(playerData.getTeamID());
				//Kill the player if they are in a redrover.
				if (entityTeam != null) {
					entityTeam.killPlayer(player.getUniqueId());
				}
			}

			if (match.getMatchState() != MatchState.ENDING) {
				if (secondlife.network.vituz.handlers.data.PlayerData.getByName(player.getName()) != null) {
					if (!VituzAPI.getRankName(player.getName()).equals("TrialMod")) {
						if (!match.haveSpectated(player.getUniqueId())) {
							match.broadcast(CC.SECONDARY + player.getName() + CC.PRIMARY + " is no longer spectating.");
							match.addHaveSpectated(player.getUniqueId());
						}
					}
				}
			}
		}

		this.spectators.remove(player.getUniqueId());
		PracticeData.sendToSpawnAndReset(player);
	}

	public void pickPlayer(Match match) {
		Player playerA = this.plugin.getServer().getPlayer(match.getTeams().get(0).getAlivePlayers().get(0));
		PracticeData playerDataA = PracticeData.getByName(playerA.getName());
		if (playerDataA.getPlayerState() != PlayerState.FIGHTING) {
			playerA.teleport(match.getArena().getA().toBukkitLocation());
			PlayerUtil.clearPlayer(playerA);
			if (match.getKit().isCombo()) {
				playerA.setMaximumNoDamageTicks(3);
			}
			this.plugin.getMatchManager().giveKits(playerA, match.getKit());
			playerDataA.setPlayerState(PlayerState.FIGHTING);
		}
		Player playerB = this.plugin.getServer().getPlayer(match.getTeams().get(1).getAlivePlayers().get(0));
		PracticeData playerDataB = PracticeData.getByName(playerB.getName());

		if (playerDataB.getPlayerState() != PlayerState.FIGHTING) {
			playerB.teleport(match.getArena().getB().toBukkitLocation());
			PlayerUtil.clearPlayer(playerB);
			if (match.getKit().isCombo()) {
				playerB.setMaximumNoDamageTicks(3);
			}
			this.plugin.getMatchManager().giveKits(playerB, match.getKit());
			playerDataB.setPlayerState(PlayerState.FIGHTING);
		}

		for (MatchTeam team : match.getTeams()) {
			for (UUID uuid : team.getAlivePlayers()) {
				Player player = this.plugin.getServer().getPlayer(uuid);

				if (player != null) {
					if (!playerA.equals(player) && !playerB.equals(player)) {
						playerA.hidePlayer(player);
						playerB.hidePlayer(player);
					}
				}
			}
		}
		playerA.showPlayer(playerB);
		playerB.showPlayer(playerA);

		match.broadcast(CC.SECONDARY + playerA.getName() + CC.PRIMARY + " vs. " + CC.SECONDARY + playerB.getName());
	}

	public void saveRematches(Match match) {
		if (match.isParty() || match.isFFA()) {
			return;
		}
		UUID playerOne = match.getTeams().get(0).getLeader();
		UUID playerTwo = match.getTeams().get(1).getLeader();

		if(playerOne == null || playerTwo == null) return;

		PracticeData dataOne = PracticeData.getByName(Bukkit.getPlayer(playerOne).getName());
		PracticeData dataTwo = PracticeData.getByName(Bukkit.getPlayer(playerTwo).getName());

		if (dataOne != null) {
			this.rematchUUIDs.put(playerOne, playerTwo);
			InventorySnapshot snapshot = match.getSnapshot(playerTwo);
			if (snapshot != null) {
				this.rematchInventories.put(playerOne, snapshot.getSnapshotId());
			}
			if (dataOne.getRematchID() > -1) {
				this.plugin.getServer().getScheduler().cancelTask(dataOne.getRematchID());
			}
			dataOne.setRematchID(
					this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new RematchRunnable(playerOne), 20L * 30L));
		}
		if (dataTwo != null) {
			this.rematchUUIDs.put(playerTwo, playerOne);
			InventorySnapshot snapshot = match.getSnapshot(playerOne);
			if (snapshot != null) {
				this.rematchInventories.put(playerTwo, snapshot.getSnapshotId());
			}
			if (dataTwo.getRematchID() > -1) {
				this.plugin.getServer().getScheduler().cancelTask(dataTwo.getRematchID());
			}
			dataTwo.setRematchID(
					this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new RematchRunnable(playerTwo), 20L * 30L));
		}
	}

	public void removeRematch(UUID uuid) {
		this.rematchUUIDs.remove(uuid);
		this.rematchInventories.remove(uuid);
	}

	public UUID getRematcher(UUID uuid) {
		return this.rematchUUIDs.get(uuid);
	}

	public UUID getRematcherInventory(UUID uuid) {
		return this.rematchInventories.get(uuid);
	}

	public boolean isRematching(UUID uuid) {
		return this.rematchUUIDs.containsKey(uuid);
	}

}
