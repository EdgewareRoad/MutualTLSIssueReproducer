package mutualtlsissue;

import java.io.File;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Enumeration;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import ch.qos.logback.core.joran.conditional.ElseAction;

@SpringBootApplication
public class Application implements CommandLineRunner {
	private Logger logger = LoggerFactory.getLogger(Application.class);
	private ApplicationConfiguration configuration = null;

	@Autowired
	public void setConfiguration(ApplicationConfiguration value) {
		configuration = value;
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(String... args) throws InterruptedException {
		logger.debug("run() method called");
		if (configuration != null && configuration.getExternalWebService() != null) {
			// Ugly sleep is here just to delay calling the server in case it's loaded inside a Docker composition and we 
			// want to give the maximum chance to let the server start fully before we call it.
			// Not production code quality, sure, but this is just a demonstrator of the problem.
			Thread.sleep(2000);
			SSLContext sslContext = getSSLContext();
			Registry<ConnectionSocketFactory> socketFactoryRegistry;
			if (sslContext != null)
			{
				SSLConnectionSocketFactory sslConSocFactory = new SSLConnectionSocketFactory(sslContext, new DefaultHostnameVerifier());
				socketFactoryRegistry
					= RegistryBuilder.<ConnectionSocketFactory> create()
						.register("http", PlainConnectionSocketFactory.INSTANCE)
						.register("https", sslConSocFactory)
						.build();
			}
			else
			{
				socketFactoryRegistry
					= RegistryBuilder.<ConnectionSocketFactory> create()
						.register("http", PlainConnectionSocketFactory.INSTANCE)
						.build();
			}
			PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

			HttpClient httpClient = HttpClients.custom()
					.setConnectionManager(connectionManager)
					.build();

			logger.info("URL to call is {}", configuration.getExternalWebService());
			HttpGet getReq = new HttpGet(configuration.getExternalWebService());
			try {
				HttpResponse response = httpClient.execute(getReq);

				logger.info("HTTP response code is {}", response.getStatusLine().getStatusCode());
			} catch (Exception e) {
				logger.error("Exception thrown in HTTP GET : {}", e.toString());
			}
		} else {
			logger.error("Can't find URL to call");
		}
	}

	private SSLContext getSSLContext() {
		final String logErrorTemplate = "getSSLContext error : {}";
		SSLContext sslContext = null;

		// OK, so we must build a custom trust and/or key store. Note that setting a key
		// store alone without a trust store makes no sense though.
		// If this is the case, the default JRE trust store should be explicitly set and
		// used here.
		SSLContextBuilder sslContextBuilder = SSLContexts.custom();

		if (configuration.getTrustStoreFilePath() != null && configuration.getTrustStoreFilePath().length() > 0) {
			logger.info("getSSLContext - Loading trust store {}", configuration.getTrustStoreFilePath());
			File fileTrustStore = new File(configuration.getTrustStoreFilePath());

			if (!fileTrustStore.exists()) {
				String error = String.format("Trust store does not exist: %s", configuration.getTrustStoreFilePath());
				logger.error(logErrorTemplate, error);
				return null;
			} else if (!fileTrustStore.canRead()) {
				String error = String.format("Trust store cannot be read: %s", configuration.getTrustStoreFilePath());
				logger.error(logErrorTemplate, error);
				return null;
			} else {
				try {
					if (configuration.getTrustStorePassword() == null
							|| configuration.getTrustStorePassword().isEmpty()) {
						logger.info("getSSLContext - Trust store has no password defined");
						sslContextBuilder = sslContextBuilder.loadTrustMaterial(fileTrustStore);
					} else {
						logger.info("getSSLContext - Trust store has password defined");
						sslContextBuilder = sslContextBuilder.loadTrustMaterial(fileTrustStore,
								configuration.getTrustStorePassword().toCharArray());
					}
				} catch (Exception trustStoreLoadException) {
					String error = String.format("Trust store errors in load: %s: %s",
							configuration.getTrustStoreFilePath(), trustStoreLoadException.getMessage());
					logger.error(logErrorTemplate, error);
					return null;
				}
			}
		}
		else
		{
			logger.info("No trust store set");
		}

		if (configuration.getKeyStoreFilePath() != null && configuration.getKeyStoreFilePath().length() > 0) {
			logger.info("getSSLContext - Loading key store {}", configuration.getKeyStoreFilePath());
			File fileKeyStore = new File(configuration.getKeyStoreFilePath());

			if (!fileKeyStore.exists()) {
				String error = String.format("Key store does not exist: %s", configuration.getKeyStoreFilePath());
				logger.error(logErrorTemplate, error);
				return null;
			} else if (!fileKeyStore.canRead()) {
				String error = String.format("Key store cannot be read: %s", configuration.getKeyStoreFilePath());
				logger.error(logErrorTemplate, error);
				return null;
			} else if (configuration.getKeyStorePassword() == null || configuration.getKeyStorePassword().isBlank()) {
				String error = "Key store password is not set";
				logger.error(logErrorTemplate, error);
				return null;
			} else if (configuration.getKeyStoreAlias() == null || configuration.getKeyStoreAlias().isBlank()) {
				String error = "Alias of required client key in key store is not set";
				logger.error(logErrorTemplate, error);
				return null;
			} else {
				try {
					KeyStore keyStore = KeyStore.getInstance(fileKeyStore,
							configuration.getKeyStorePassword().toCharArray());
					Enumeration<String> aliases = keyStore.aliases();
					while (aliases.hasMoreElements()) {
						String alias = aliases.nextElement();
						logger.info("Key store certificate chain for alias {}", alias);
						for (Certificate cert : keyStore.getCertificateChain(alias)) {
							logger.info("Cert in chain: {}", cert);
						}
					}

					if (configuration.getKeyStoreKeyPassword() == null
							|| configuration.getKeyStoreKeyPassword().isBlank()) {
						// The default is that the key store password for p12 files is used as the key
						// password too.
						sslContextBuilder = sslContextBuilder.loadKeyMaterial(fileKeyStore,
								configuration.getKeyStorePassword().toCharArray(),
								configuration.getKeyStorePassword().toCharArray(),
								new ClientKeyStrategy(configuration.getKeyStoreAlias()));
					} else {
						// Only needed in rare cases where old style Java keystores are used.
						sslContextBuilder = sslContextBuilder.loadKeyMaterial(fileKeyStore,
								configuration.getKeyStorePassword().toCharArray(),
								configuration.getKeyStoreKeyPassword().toCharArray(),
								new ClientKeyStrategy(configuration.getKeyStoreAlias()));
					}
				} catch (Exception keyStoreLoadException) {
					String error = String.format("Key store errors in load: %s: %s",
							configuration.getKeyStoreFilePath(), keyStoreLoadException.toString());
					logger.error(logErrorTemplate, error);
					return null;
				}
			}
		}
		else
		{
			logger.info("No key store set");
		}

		try {
			logger.info("getSSLContext - Building SSLContext");

			sslContext = sslContextBuilder.build();
		} catch (Exception sslContextBuildException) {
			String error = String.format("Could not build SSLContext: %s", sslContextBuildException.getMessage());
			logger.error(logErrorTemplate, error);
			return null;
		}

		return sslContext;
	}
}
