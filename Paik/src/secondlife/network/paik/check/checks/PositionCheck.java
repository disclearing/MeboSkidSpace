package secondlife.network.paik.check.checks;

import secondlife.network.paik.Paik;
import secondlife.network.paik.check.AbstractCheck;
import secondlife.network.paik.handlers.data.PlayerData;
import secondlife.network.vituz.utilties.update.PositionUpdate;

public abstract class PositionCheck extends AbstractCheck<PositionUpdate> {

    public PositionCheck(Paik plugin, PlayerData playerData, String name) {
        super(plugin, playerData, PositionUpdate.class, name);
    }
}
