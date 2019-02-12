package secondlife.network.paik.check.impl.autoclicker;

import java.util.Deque;
import java.util.LinkedList;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayInArmAnimation;
import net.minecraft.server.v1_8_R3.PacketPlayInBlockDig;
import net.minecraft.server.v1_8_R3.PacketPlayInFlying;
import org.bukkit.entity.Player;
import secondlife.network.paik.Paik;
import secondlife.network.paik.utilties.events.player.PlayerAlertEvent;
import secondlife.network.paik.check.checks.PacketCheck;
import secondlife.network.paik.handlers.data.PlayerData;

public class AutoClickerH extends PacketCheck {

	private Deque<Integer> recentCounts = new LinkedList<>();
	private int flyingCount;
	private boolean release;

	public AutoClickerH(Paik plugin, PlayerData playerData) {
		super(plugin, playerData, "Auto-Clicker (Check 8)");
	}

	@Override
	public void handleCheck(Player player, Packet packet) {
		if(packet instanceof PacketPlayInArmAnimation && !this.playerData.isDigging() &&
		    !this.playerData.isPlacing() &&
		    System.currentTimeMillis() - this.playerData.getLastDelayedMovePacket() > 220L &&
		    this.playerData.getLastMovePacket() != null &&
		    System.currentTimeMillis() - this.playerData.getLastMovePacket().getTimestamp() < 110L &&
		    !this.playerData.isFakeDigging()) {

			if(this.flyingCount < 10) {
				if(this.release) {
					this.release = false;
					this.flyingCount = 0;
					return;
				}

				this.recentCounts.add(this.flyingCount);

				if(this.recentCounts.size() == 100) {
					double average = 0.0;

					for(int i : this.recentCounts) {
						average += i;
					}

					average /= this.recentCounts.size();
					double stdDev = 0.0;

					for(int j : this.recentCounts) {
						stdDev += Math.pow(j - average, 2.0);
					}

					stdDev /= this.recentCounts.size();
					stdDev = Math.sqrt(stdDev);

					double vl = this.getVl();

					if(stdDev < 0.45) {
						if((vl += 1.4) >= 4.0) {
							this.alert(PlayerAlertEvent.AlertType.EXPERIMENTAL, player, String.format(
									"STD %.2f. VL %.2f.", stdDev, vl), false);
						}
					} else {
						vl -= 0.8;
					}

					this.setVl(vl);
					this.recentCounts.clear();
				}
			}

			this.flyingCount = 0;
		} else if(packet instanceof PacketPlayInFlying) {
			++this.flyingCount;
		} else if(packet instanceof PacketPlayInBlockDig && ((PacketPlayInBlockDig) packet).c() == PacketPlayInBlockDig.EnumPlayerDigType.RELEASE_USE_ITEM) {
			this.release = true;
		}
	}
}
