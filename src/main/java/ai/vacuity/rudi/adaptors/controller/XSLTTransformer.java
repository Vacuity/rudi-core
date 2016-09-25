package ai.vacuity.rudi.adaptors.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.json.JSONObject;
import org.json.XML;

/**
 *
 * @author M.Vasudevarao
 * @author smonroe
 */
public class XSLTTransformer {
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
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "<root>\n" + XML.toString(new JSONObject(json))
				+ "</root>\n";
	}

	public static void transform(String json, String xslt)
			throws TransformerConfigurationException, TransformerException, IOException {

		String xml = transform(json);
		System.out.println("The generated XML file is:\n" + xml);
		TransformerFactory factory = TransformerFactory.newInstance();
		// File f = new File(xslt);
		URL oracle = new URL(xslt);
		BufferedReader xslin = new BufferedReader(new InputStreamReader(oracle.openStream()));
		StreamSource xslStream = new StreamSource(xslin);

		Transformer transformer = factory.newTransformer(xslStream);
		StringReader srxml = new StringReader(xml);

		String fileStr = xslt.replace(".xsl", ".rdf").replace("http://localhost:8080/rudi-adaptors/a/",
				"/Users/smonroe/workspace/rudi-adaptors/src/main/webapp/WEB-INF/resources/adaptors/");
		File f = new File(fileStr);
		if (!f.getParentFile().exists())
			f.getParentFile().mkdirs();
		StreamSource in = new StreamSource(srxml);
		StreamResult out = new StreamResult(f);
		System.out.println("The generated RDF file is:\n");
		transformer.transform(in, out);
		// QuadStore.main(new String[] {});

	}
}