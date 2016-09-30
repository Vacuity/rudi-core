package ai.vacuity.rudi.adaptors.bo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

import com.google.api.client.http.GenericUrl;

import ai.vacuity.rudi.adaptors.interfaces.IResponseProcessor;
import ai.vacuity.rudi.adaptors.interfaces.ITemplateProcessor;

public class Config {
	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(Config.class);

	private static final String FILE_NAME = "api.config";

	private String key = "";
	private String id = "";
	private String token = "";
	private GenericUrl url = null;
	private ITemplateProcessor templateProcessor = null;
	private IResponseProcessor responseProcessor = null;

	final static HashMap<String, Config> map = new HashMap<String, Config>();
	final static Properties settings = new Properties();
	final static Vector<String> labels = new Vector<String>();

	static final String PROPERTY_SUFFIX_URL = "url";
	static final String PROPERTY_SUFFIX_ID = "id";
	static final String PROPERTY_SUFFIX_KEY = "key";
	static final String PROPERTY_SUFFIX_TOKEN = "token";
	static final String PROPERTY_SUFFIX_TEMPLATE_PROCESSOR = "tp";
	static final String PROPERTY_SUFFIX_RESPONSE_PROCESSOR = "rp";

	static {
		ClassLoader cLoader = Config.class.getClassLoader();
		InputStream is = cLoader.getResourceAsStream(Config.FILE_NAME);
		try {
			Config.getSettings().load(is);
		}
		catch (IOException ioex) {
			ioex.printStackTrace();
		}
	}

	public boolean hasId() {
		return StringUtils.isNotBlank(getId());
	}

	public boolean hasTemplateProcessor() {
		return getTemplateProcessor() != null;
	}

	public boolean hasResponseProcessor() {
		return getResponseProcessor() != null;
	}

	public boolean hasToken() {
		return StringUtils.isNotBlank(getToken());
	}

	public boolean hasKey() {
		return StringUtils.isNotBlank(getKey());
	}

	public GenericUrl getUrl() {
		return url;
	}

	public void setUrl(GenericUrl url) {
		this.url = url;
	}

	public static Properties getSettings() {
		return settings;
	}

	public static Vector<String> getLabels() {
		return labels;
	}

	public static final void load() {
		Enumeration<Object> keys = getSettings().keys();
		while (keys.hasMoreElements()) {
			String urlKey = ((String) keys.nextElement()).toLowerCase();
			if (urlKey.endsWith("." + Config.PROPERTY_SUFFIX_URL)) {
				String endpointLabel = urlKey.substring(0, urlKey.lastIndexOf(".url"));
				Config e = Config.add(endpointLabel);

				String url = Config.getSettings().getProperty(urlKey);
				if (StringUtils.isNotEmpty(url)) {
					e.setUrl(new GenericUrl(url));
				}
				e.setId(Config.getSettings().getProperty(endpointLabel + "." + Config.PROPERTY_SUFFIX_ID));
				e.setKey(Config.getSettings().getProperty(endpointLabel + "." + Config.PROPERTY_SUFFIX_KEY));
				e.setToken(Config.getSettings().getProperty(endpointLabel + "." + Config.PROPERTY_SUFFIX_TOKEN));
				String tpStr = Config.getSettings().getProperty(endpointLabel + "." + Config.PROPERTY_SUFFIX_TEMPLATE_PROCESSOR);
				String rpStr = Config.getSettings().getProperty(endpointLabel + "." + Config.PROPERTY_SUFFIX_RESPONSE_PROCESSOR);
				if (StringUtils.isNotBlank(tpStr)) {
					try {
						Class clazz = Class.forName(tpStr);
						ITemplateProcessor tp = (ITemplateProcessor) clazz.newInstance();
						e.setTemplateProcessor(tp);

						clazz = Class.forName(rpStr);
						IResponseProcessor rp = (IResponseProcessor) clazz.newInstance();
						e.setResponseProcessor(rp);
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
				}
			}
		}
	}

	public static Config add(String endpoint) {
		Config.getLabels().add(endpoint);
		Config e = new Config();
		Config.getMap().put(endpoint, e);
		return e;
	}

	public static HashMap<String, Config> getMap() {
		return map;
	}

	/**
	 * Call limit, expressed as [total_calls,time_window,time_unit]
	 */
	private int[][] limit = new int[0][0];

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

	public ITemplateProcessor getTemplateProcessor() {
		return templateProcessor;
	}

	public void setTemplateProcessor(ITemplateProcessor processor) {
		this.templateProcessor = processor;
	}

	public IResponseProcessor getResponseProcessor() {
		return responseProcessor;
	}

	public void setResponseProcessor(IResponseProcessor responseProcessor) {
		this.responseProcessor = responseProcessor;
	}

}
