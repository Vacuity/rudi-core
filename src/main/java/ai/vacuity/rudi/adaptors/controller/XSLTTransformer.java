package ai.vacuity.rudi.adaptors.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.UUID;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.LoggerFactory;

import ai.vacuity.rudi.adaptors.data.QuadStore;

/**
 *
 * @author M.Vasudevarao
 * @author smonroe
 */
public class XSLTTransformer {
	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(XSLTTransformer.class);

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		if (args.length != 3) {
			System.err.println("give command as follows : ");
			System.err.println("XSLTTest data.xml converted.xsl converted.html");
			return;
		}

		String json = "";

		// String dataXML = args[0];
		String xslt = args[1];
		String rdf = args[2];

		XSLTTransformer st = new XSLTTransformer();
		// try
		// {
		// st.transform(json, xslt, rdf);
		// }
		// catch (TransformerConfigurationException e)
		// {
		// System.err.println("TransformerConfigurationException");
		// System.err.println(e);
		// }
		// catch (TransformerException e)
		// {
		// System.err.println("TransformerException");
		// System.err.println(e);
		// }
	}

	public static String transform(String json) {
		if (json.startsWith("[") && json.endsWith("]")) {
			json = "{\"items\":\n" + json + "\n}";
		}
		if (StringUtils.isBlank(json)) return null;
		logger.debug("Truncated JSON:\n" + json);
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "<root>\n" + XML.toString(new JSONObject(json)) + "</root>\n";
	}

	public static void transform(String json, String xslt) throws TransformerConfigurationException, TransformerException, IOException {

		logger.debug("Retrieved JSON:\n" + json);
		String xml = transform(json);
		if (xml == null) return;
		logger.debug("The generated XML file is:\n" + xml);
		TransformerFactory factory = TransformerFactory.newInstance();
		// File f = new File(xslt);
		URL oracle = new URL(xslt);
		BufferedReader xslin = new BufferedReader(new InputStreamReader(oracle.openStream()));
		StreamSource xslStream = new StreamSource(xslin);

		Transformer transformer = factory.newTransformer(xslStream);
		StringReader srxml = new StringReader(xml);
		UUID uuid = UUID.randomUUID();

		String filePathStr = xslt.replace(".xsl", "-" + uuid + ".rdf").replace("http://localhost:8080/rudi-adaptors/a/", "/Users/smonroe/workspace/rudi-adaptors/src/main/webapp/WEB-INF/resources/adaptors/");
		String fxmlStr = xslt.replace(".xsl", "-" + uuid + ".xml").replace("http://localhost:8080/rudi-adaptors/a/", "/Users/smonroe/workspace/rudi-adaptors/src/main/webapp/WEB-INF/resources/adaptors/");
		File fxml = new File(fxmlStr);
		FileWriter fw = new FileWriter(fxml);
		fw.write(xml);
		fw.close();
		File f = new File(filePathStr);
		if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
		StreamSource in = new StreamSource(srxml);
		StreamResult out = new StreamResult(f);
		logger.debug("The generated RDF file is:\n");
		transformer.transform(in, out);

		QuadStore.addToRepository(filePathStr, "http://tryrudi.io/rdf/demo/");

		// QuadStore.main(new String[] {});

	}
}