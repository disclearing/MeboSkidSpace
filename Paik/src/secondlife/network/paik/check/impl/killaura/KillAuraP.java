package secondlife.network.paik.check.impl.killaura;

import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayInClientCommand;
import net.minecraft.server.v1_8_R3.PacketPlayInFlying;
import net.minecraft.server.v1_8_R3.PacketPlayInUseEntity;
import org.bukkit.entity.Player;
import secondlife.network.paik.Paik;
import secondlife.network.paik.check.checks.PacketCheck;
import secondlife.network.paik.handlers.data.PlayerData;
import secondlife.network.paik.utilties.events.player.PlayerAlertEvent;

public class KillAuraP extends PacketCheck {

    private boolean sent;
    
    public KillAuraP(Paik plugin, PlayerData playerData) {
        super(plugin, playerData, "Kill-Aura (Check 16)");
    }
    
    @Override
    public void handleCheck(Player player, Packet packet) {
        if(packet instanceof PacketPlayInClientCommand && ((PacketPlayInClientCommand)packet).a() == PacketPlayInClientCommand.EnumClientCommand.OPEN_INVENTORY_ACHIEVEMENT) {
            if(this.sent && this.alert(PlayerAlertEvent.AlertType.RELEASE, player, "", true)) {
                int violations = this.playerData.getViolations(this, 60000L);

                if(!this.playerData.isBanning() && violations > 2) {
                    this.ban(player);
                }
            }
        } else if(packet instanceof PacketPlayInUseEntity && ((PacketPlayInUseEntity)packet).a() == PacketPlayInUseEntity.EnumEntityUseAction.ATTACK) {
            this.sent = true;
        } else if(packet instanceof PacketPlayInFlying) {
            this.sent = false;
        }
    }
}
