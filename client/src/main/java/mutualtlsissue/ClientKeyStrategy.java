package mutualtlsissue;

import java.net.Socket;
import java.util.Map;

import org.apache.http.ssl.PrivateKeyDetails;
import org.apache.http.ssl.PrivateKeyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientKeyStrategy implements PrivateKeyStrategy {
    private final Logger logger = LoggerFactory.getLogger(ClientKeyStrategy.class);
    private final String clientAlias;

    public ClientKeyStrategy(String clientAlias) {
        this.clientAlias = clientAlias;
    }

	@Override
	public String chooseAlias(Map<String, PrivateKeyDetails> aliases, Socket socket) {
        if (aliases == null || aliases.size() == 0) return null;
        for(String alias : aliases.keySet())
        {
            if (alias.equals(clientAlias))
            {
                logger.debug("Selected clientAlias {}", clientAlias);
                return clientAlias;            }
        }
        logger.error("Client alias not found so returning null. Client certificate may be missing from keystore.");
        return null;
	}

}