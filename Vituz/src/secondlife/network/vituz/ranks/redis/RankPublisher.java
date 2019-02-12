package secondlife.network.vituz.ranks.redis;

import redis.clients.jedis.Jedis;
import secondlife.network.vituz.Vituz;
import secondlife.network.vituz.utilties.ConfigFile;

public class RankPublisher {

	public static void write(String message) {
		Jedis jedis = null;
		
		try {
			jedis = Vituz.getInstance().getDatabaseManager().getPool().getResource();
			
			if(Vituz.getInstance().getConfig().getBoolean("DATABASE.AUTHENTICATION.ENABLED")) {
				jedis.auth(Vituz.getInstance().getConfig().getString("DATABASE.AUTHENTICATION.PASSWORD"));
			}
			
			jedis.publish("ranks", message);
		} finally {
			if(jedis != null) jedis.close();
		}
	}
}
