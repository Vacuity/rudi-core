package ai.vacuity.rudi.adaptors.bo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

import com.google.api.client.http.GenericUrl;

import ai.vacuity.rudi.adaptors.interfaces.IResponseModule;
import ai.vacuity.rudi.adaptors.interfaces.ITemplateModule;
import ai.vacuity.rudi.sensor.DistTutorial;
import ai.vacuity.rudi.sensor.Router;
import ai.vacuity.utils.OSValidator;
import rice.environment.Environment;

public class Config {
	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(Config.class);

	private static final String FILE_NAME = "settings.ini";

	private String key = "";
	private String id = "";
	private String token = "";
	private int port = 80;
	private String host = "localhost";
	private String path = "";
	private String repoType = "sparql";
	private boolean secure = false;
	private String username = null;
	private String password = null;
	private ITemplateModule templateModule = null;
	private IResponseModule responseModule = null;

	final static HashMap<String, Config> map = new HashMap<String, Config>();
	final static Properties settings = new Properties();
	final static Vector<String> labels = new Vector<String>();

	public static final String PROPERTY_RUDI_SYSTEM = "rudi.system";
	public static final String PROPERTY_RUDI_CONTENT = "rudi.content";
	public static final String PROPERTY_RUDI_ENDPOINT = "rudi.endpoint";

	public static final String PROPERTY_SUFFIX_REPO_TYPE = "type";

	public static final String REPO_TYPE_VIRTUOSO = "virtuoso";
	public static final String REPO_TYPE_JENA = "jena";
	public static final String REPO_TYPE_RDF4J = "rdf4j";
	public static final String REPO_TYPE_SPARQL = "sparql";

	public static final String PROPERTY_SUFFIX_USER = "user";
	public static final String PROPERTY_SUFFIX_PASS = "pass";
	public static final String PROPERTY_SUFFIX_LAZYADD = "lazyAdd";
	public static final String PROPERTY_SUFFIX_DEFAULTGRAPH = "defaultGraph";

	public static final String PROPERTY_P2P_PEERS = "p2p.peers";
	public static final String PROPERTY_P2P_PORT = "p2p.port";
	public static final String PROPERTY_SUFFIX_HOST = "host";
	public static final String PROPERTY_SUFFIX_PORT = "port";
	public static final String PROPERTY_SUFFIX_PATH = "path";
	public static final String PROPERTY_SUFFIX_SECURE = "secure";
	static final String PROPERTY_SUFFIX_ID = "id";
	static final String PROPERTY_SUFFIX_KEY = "key";
	static final String PROPERTY_SUFFIX_TOKEN = "token";
	static final String PROPERTY_SUFFIX_TEMPLATE_MODULE = "tm";
	static final String PROPERTY_SUFFIX_RESPONSE_MODULE = "rm";

	/**
	 * Call limit, expressed as [total_calls,time_window,time_unit]
	 */
	private int[][] limit = new int[0][0];

	static {

		ClassLoader cLoader = Config.class.getClassLoader();
		try {
			InputStream is = cLoader.getResourceAsStream(Config.FILE_NAME);
			File file = new File(System.getProperty("user.home") + File.separator + ".rudi" + File.separator + Config.FILE_NAME);
			if (file.exists()) {
				is = new FileInputStream(file);
			}
			else {
				try {
					FileUtils.forceMkdir(file.getParentFile());
				}
				catch (IOException ex) {
					logger.error(ex.getMessage(), ex);
				}

				if (OSValidator.isWindows()) {
					logger.debug("This is Windows");
					if (!file.exists()) {
						try {
							Files.setAttribute(file.getParentFile().toPath(), "dos:hidden", true);
						}
						catch (IOException ex) {
							logger.error(ex.getMessage(), ex);
						}
					}
				}

				else if (OSValidator.isMac()) {
					logger.debug("This is Mac");
				}
				else if (OSValidator.isUnix()) {
					logger.debug("This is Unix");
				}
				else if (OSValidator.isSolaris()) {
					logger.debug("This is Solaris");
				}

				file.createNewFile();

				try (FileOutputStream output = new FileOutputStream(file)) {
					int len;
					byte[] buffer = new byte[8 * 1024];
					InputStream input = cLoader.getResourceAsStream(Config.FILE_NAME);
					if (input != null) {
						while ((len = input.read(buffer, 0, buffer.length)) > 0) {
							output.write(buffer, 0, len);
						}
					}
					else {
						throw new MissingResourceException("Could not find the start up properties file '" + Config.FILE_NAME + "'", "Program Startup", "Configuration File");
					}
				}
				catch (Exception ex) {
					logger.error(ex.getMessage(), ex);
				}
			}

			if (is != null) {
				Config.getSettings().load(is);
				Config.load();
			}
			else {
				throw new MissingResourceException("Could not find the start up properties file '" + Config.FILE_NAME + "'", "Program Startup", "Configuration File");
			}
		}
		catch (IOException ioex) {
			logger.error(ioex.getMessage(), ioex);
		}
	}

	// these fields depend on the block above
	public static final String RUDI_HOST = getSettings().getProperty("rudi.host");
	public static final String RUDI_PORT_STR = getSettings().getProperty("rudi.port");
	public static final String RUDI_PATH = getSettings().getProperty("rudi.path");
	public static final String RUDI_SECURE = getSettings().getProperty("rudi.secure");

	public static final String SPARQL_ENDPOINT_RESPONSES_LABEL = "rudi.repo.responses";

	public static final String SPARQL_ENDPOINT_ALERTS_LABEL = "rudi.repo.alerts";

	public static final String SPARQL_ENDPOINT_VIA_LABEL = "rudi.repo.via";

	public static final String PREFIX_RUDI_REPO = "rudi.repo";

	public static final String SPARQL_ENDPOINT_RESPONSES = getSettings().getProperty(Config.SPARQL_ENDPOINT_RESPONSES_LABEL);

	public static final String SPARQL_ENDPOINT_ALERTS = getSettings().getProperty(Config.SPARQL_ENDPOINT_RESPONSES_LABEL);

	public static final String SPARQL_ENDPOINT_VIA = getSettings().getProperty(Config.SPARQL_ENDPOINT_RESPONSES_LABEL);

	public static final String DIR_ALERTS = (getSettings().containsKey(Config.PROPERTY_RUDI_SYSTEM)) ? getSettings().getProperty(Config.PROPERTY_RUDI_SYSTEM) + "data/" : System.getProperty("user.home") + File.separator + "alerts/";

	public static final String DIR_RESPONSES = (getSettings().containsKey(Config.PROPERTY_RUDI_SYSTEM)) ? getSettings().getProperty(Config.PROPERTY_RUDI_SYSTEM) + "responses/" : System.getProperty("user.home") + File.separator + "responses/";

	public static final String DIR_LISTENERS = (getSettings().containsKey(Config.PROPERTY_RUDI_CONTENT)) ? getSettings().getProperty(Config.PROPERTY_RUDI_CONTENT) + "listeners/" : System.getProperty("user.home") + File.separator + "listeners/";

	// public String getRudiHost() {
	// return Config.RUDI_HOST;
	// }

	public static GenericUrl getRudiContainer() {
		int rudiPort = 80;
		try {
			rudiPort = Integer.parseInt(RUDI_PORT_STR);
		}
		catch (NumberFormatException nfex) {
			// port not required
		}
		return new GenericUrl((Boolean.parseBoolean(RUDI_SECURE) ? "https://" : "http://") + RUDI_HOST + ((rudiPort != 80) ? ":" + rudiPort : ""));
	}

	public static String getRudiEndpoint() {
		int rudiPort = 80;
		try {
			rudiPort = Integer.parseInt(RUDI_PORT_STR);
		}
		catch (NumberFormatException nfex) {
			// port not required
		}
		return (Boolean.parseBoolean(RUDI_SECURE) ? "https://" : "http://") + RUDI_HOST + ((rudiPort != 80) ? ":" + rudiPort : "") + RUDI_PATH;
	}

	public boolean hasId() {
		return StringUtils.isNotBlank(getId());
	}

	public boolean hasTemplateModule() {
		return getTemplateModule() != null;
	}

	public boolean hasResponseModule() {
		return getResponseModule() != null;
	}

	public boolean hasToken() {
		return StringUtils.isNotBlank(getToken());
	}

	public boolean hasKey() {
		return StringUtils.isNotBlank(getKey());
	}

	public List<GenericUrl> getSandboxedEndpoints() {
		List<GenericUrl> l = new ArrayList<GenericUrl>();
		l.add(new GenericUrl((isSecure() ? "https://" : "http://") + getHost() + ":" + getPort() + (StringUtils.isNotBlank(getPath()) ? getPath() : "")));
		if (getPort() == 80) {
			l.add(new GenericUrl((isSecure() ? "https://" : "http://") + getHost() + (StringUtils.isNotBlank(getPath()) ? getPath() : "")));
		}
		return l;
	}

	private static Properties getSettings() {
		return settings;
	}

	public static Vector<String> getLabels() {
		return labels;
	}

	public static final void load() {
		String peersStr = getSettings().getProperty(Config.PROPERTY_P2P_PEERS);
		if (StringUtils.isNotEmpty(peersStr)) {
			StringTokenizer st = new StringTokenizer(getSettings().getProperty(Config.PROPERTY_P2P_PEERS));
			InetSocketAddress[] peers = new InetSocketAddress[st.countTokens()];
			while (st.hasMoreTokens()) {
				String peer = st.nextToken();
				String host = getSettings().getProperty(peer + "." + "host");
				int port = Router.PORT_DEFAULT;
				try {
					port = Integer.parseInt(getSettings().getProperty(peer + "." + "port"));
				}
				catch (NumberFormatException nfex) {

				}
				Router.add(new InetSocketAddress(host, port));
			}
			if (peers.length > 0) {
				int localPort = Router.PORT_DEFAULT;
				try {
					localPort = Integer.parseInt(getSettings().getProperty(Config.PROPERTY_P2P_PORT));
				}
				catch (NumberFormatException nfex) {

				}
				// Loads pastry settings
				Environment env = new Environment();

				// disable the UPnP setting (in case you are testing this on a NATted LAN)
				env.getParameters().setString("nat_search_policy", "never");
				try {
					for (InetSocketAddress p : Router.getPeers()) {
						new DistTutorial(localPort, p, env);
					}
					// new Router(localPort, peers);
				}
				catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
		Enumeration<Object> keys = getSettings().keys();
		while (keys.hasMoreElements()) {
			String key = ((String) keys.nextElement()).toLowerCase();
			if (key.endsWith("." + Config.PROPERTY_SUFFIX_HOST)) {
				String configLabel = key.substring(0, key.lastIndexOf("." + Config.PROPERTY_SUFFIX_HOST));
				Config config = Config.add(configLabel);

				String host = Config.getSettings().getProperty(key);

				if (StringUtils.isNotEmpty(host)) {
					config.setHost(host);
				}
				else {
					continue;
				}
				config.setRepoType(Config.getSettings().getProperty(configLabel + "." + Config.PROPERTY_SUFFIX_REPO_TYPE));
				config.setUserName(Config.getSettings().getProperty(configLabel + "." + Config.PROPERTY_SUFFIX_USER));
				config.setPassword(Config.getSettings().getProperty(configLabel + "." + Config.PROPERTY_SUFFIX_PASS));
				config.setPath(Config.getSettings().getProperty(configLabel + "." + Config.PROPERTY_SUFFIX_PATH));
				config.setId(Config.getSettings().getProperty(configLabel + "." + Config.PROPERTY_SUFFIX_ID));
				config.setKey(Config.getSettings().getProperty(configLabel + "." + Config.PROPERTY_SUFFIX_KEY));
				config.setToken(Config.getSettings().getProperty(configLabel + "." + Config.PROPERTY_SUFFIX_TOKEN));
				String portStr = Config.getSettings().getProperty(configLabel + "." + Config.PROPERTY_SUFFIX_PORT);
				if (StringUtils.isNotBlank(portStr)) {
					try {
						config.setPort(Integer.parseInt(portStr));
					}
					catch (NumberFormatException nfex) {
						logger.error(nfex.getMessage(), nfex);
					}
					catch (NullPointerException npex) {
						// port not required
					}
				}
				config.setSecure(Boolean.parseBoolean(Config.getSettings().getProperty(configLabel + "." + Config.PROPERTY_SUFFIX_SECURE)));
				config.setPath(Config.getSettings().getProperty(configLabel + "." + Config.PROPERTY_SUFFIX_PATH));
				config.setToken(Config.getSettings().getProperty(configLabel + "." + Config.PROPERTY_SUFFIX_TOKEN));
				config.setToken(Config.getSettings().getProperty(configLabel + "." + Config.PROPERTY_SUFFIX_TOKEN));
				String tpStr = Config.getSettings().getProperty(configLabel + "." + Config.PROPERTY_SUFFIX_TEMPLATE_MODULE);
				String rpStr = Config.getSettings().getProperty(configLabel + "." + Config.PROPERTY_SUFFIX_RESPONSE_MODULE);

				if (StringUtils.isNotBlank(tpStr)) {
					try {
						Class clazz = Class.forName(tpStr);
						ITemplateModule tp = (ITemplateModule) clazz.newInstance();
						config.setTemplateModule(tp);
					}
					catch (ClassNotFoundException cnfex) {
						logger.error(cnfex.getMessage(), cnfex);
					}
					catch (InstantiationException iex) {
						logger.error(iex.getMessage(), iex);
					}
					catch (IllegalAccessException iaex) {
						logger.error(iaex.getMessage(), iaex);
					}
					catch (NullPointerException npex) {
						// tm property is not required
					}
					try {
						Class clazz = Class.forName(rpStr);
						IResponseModule rp = (IResponseModule) clazz.newInstance();
						config.setResponseModule(rp);
					}
					catch (ClassNotFoundException cnfex) {
						logger.error(cnfex.getMessage(), cnfex);
					}
					catch (InstantiationException iex) {
						logger.error(iex.getMessage(), iex);
					}
					catch (IllegalAccessException iaex) {
						logger.error(iaex.getMessage(), iaex);
					}
					catch (NullPointerException npex) {
						// rm property is not required
					}
				}
			}
		}
	}

	public static boolean isConfigured(String label) {
		return getMap().get(label) != null;
	}

	public static Config get(String label) {
		return getMap().get(label);
	}

	public String getProperty(String key) {
		return getSettings().getProperty(key);
	}

	public static Config add(String label) {
		Config.getLabels().add(label);
		Config e = new Config();
		Config.getMap().put(label, e);
		return e;
	}

	private static HashMap<String, Config> getMap() {
		return map;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public int[][] getLimit() {
		return limit;
	}

	public void setLimit(int[][] limit) {
		this.limit = limit;
	}

	public ITemplateModule getTemplateModule() {
		return templateModule;
	}

	public void setTemplateModule(ITemplateModule templateModule) {
		this.templateModule = templateModule;
	}

	public IResponseModule getResponseModule() {
		return responseModule;
	}

	public void setResponseModule(IResponseModule responseModule) {
		this.responseModule = responseModule;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String domain) {
		this.host = domain;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isSecure() {
		return secure;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public String getRepoType() {
		return repoType;
	}

	public void setRepoType(String repoType) {
		this.repoType = repoType;
	}

	public String getUserName() {
		return username;
	}

	public void setUserName(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
