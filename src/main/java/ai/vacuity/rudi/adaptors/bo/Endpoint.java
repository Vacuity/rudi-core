package ai.vacuity.rudi.adaptors.bo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import com.google.api.client.http.GenericUrl;

public class Endpoint {

	private static final String FILENAME_CONFIG = "api.config";

	private String key = "";
	private String id = "";
	private String token = "";
	private GenericUrl url = null;

	final static HashMap<String, Endpoint> endpointMap = new HashMap<String, Endpoint>();
	final static Properties config = new Properties();
	final static Vector<String> endpointIds = new Vector<String>();

	public boolean hasId() {
		return StringUtils.isNotBlank(getId());
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

	public static Properties getConfig() {
		return config;
	}

	public static Vector<String> getEndpointids() {
		return endpointIds;
	}

	public static final void load() {
		try {
			ClassLoader cLoader = Endpoint.class.getClassLoader();
			InputStream is = cLoader.getResourceAsStream(Endpoint.FILENAME_CONFIG);
			Endpoint.getConfig().load(is);
			Enumeration<Object> keys = getConfig().keys();
			while (keys.hasMoreElements()) {
				String urlKey = ((String) keys.nextElement()).toLowerCase();
				if (urlKey.endsWith(".url")) {
					String endpointLabel = urlKey.substring(0, urlKey.lastIndexOf(".url"));
					Endpoint e = Endpoint.add(endpointLabel);

					String url = Endpoint.getConfig().getProperty(urlKey);
					if (StringUtils.isNotEmpty(url)) {
						e.setUrl(new GenericUrl(url));
					}
					e.setId(Endpoint.getConfig().getProperty(endpointLabel + ".id"));
					e.setKey(Endpoint.getConfig().getProperty(endpointLabel + ".key"));
					e.setToken(Endpoint.getConfig().getProperty(endpointLabel + ".token"));
				}
			}
		}
		catch (IOException ioex) {
			ioex.printStackTrace();
		}
	}

	public static Endpoint add(String endpoint) {
		Endpoint.getEndpointids().add(endpoint);
		Endpoint e = new Endpoint();
		Endpoint.getEndpointmap().put(endpoint, e);
		return e;
	}

	public static HashMap<String, Endpoint> getEndpointmap() {
		return endpointMap;
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

}
