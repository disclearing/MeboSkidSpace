package secondlife.network.paik.check.impl.aimassist;

import org.bukkit.entity.Player;
import secondlife.network.paik.Paik;
import secondlife.network.paik.check.checks.RotationCheck;
import secondlife.network.paik.handlers.data.PlayerData;
import secondlife.network.paik.utilties.MathUtil;
import secondlife.network.paik.utilties.events.player.PlayerAlertEvent;
import secondlife.network.vituz.utilties.update.RotationUpdate;

public class AimAssistD extends RotationCheck {

	private float lastYawRate;
	private float lastPitchRate;

	public AimAssistD(Paik plugin, PlayerData playerData) {
		super(plugin, playerData, "Aim (Check 4)");
	}

	@Override
	public void handleCheck(Player player, RotationUpdate update) {
		if(System.currentTimeMillis() - this.playerData.getLastAttackPacket() > 10000L) return;

		float diffPitch = MathUtil.getDistanceBetweenAngles(update.getTo().getPitch(), update.getFrom().getPitch());
		float diffYaw = MathUtil.getDistanceBetweenAngles(update.getTo().getYaw(), update.getFrom().getYaw());

		float diffPitchRate = Math.abs(this.lastPitchRate - diffPitch);
		float diffYawRate = Math.abs(this.lastYawRate - diffYaw);

		float diffPitchRatePitch = Math.abs(diffPitchRate - diffPitch);
		float diffYawRateYaw = Math.abs(diffYawRate - diffYaw);

		if(diffPitch < 0.009 && diffPitch > 0.001 && diffPitchRate > 1.0 && diffYawRate > 1.0 && diffYaw > 3.0 &&
		    this.lastYawRate > 1.5 && (diffPitchRatePitch > 1.0f || diffYawRateYaw > 1.0f)) {
			this.alert(PlayerAlertEvent.AlertType.EXPERIMENTAL, player, String.format(
							"DPR %.3f. DYR %.3f. LPR %.3f. LYR %.3f. DP %.3f. DY %.2f. DPRP %.3f. DYRY %.3f.",
							diffPitchRate, diffYawRate, this.lastPitchRate, this.lastYawRate, diffPitch, diffYaw,
							diffPitchRatePitch, diffYawRateYaw), true);

			if(!this.playerData.isBanning() && this.playerData.getViolations(this, 1000L * 60 * 10) > 5) {
				this.ban(player);
			}
		}

		this.lastPitchRate = diffPitch;
		this.lastYawRate = diffYaw;
	}

}
