package ai.vacuity.rudi.adaptors.controller;

import javax.xml.transform.ErrorListener;
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
public class XSLTTransformer
{
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if (args.length != 3)
        {
            System.err.println("give command as follows : ");
            System.err.println("XSLTTest data.xml converted.xsl converted.html");
            return;
        }
        
		
		String str = "";
		JSONObject json = new JSONObject(str);
		String xml = XML.toString(json);
        
        
//        String dataXML = args[0];
        String xslt = args[1];
        String rdf = args[2];

        XSLTTransformer st = new XSLTTransformer();
        try
        {
            st.transform(xml, xslt, rdf);
        }
        catch (TransformerConfigurationException e)
        {
            System.err.println("TransformerConfigurationException");
            System.err.println(e);
        }
        catch (TransformerException e)
        {
            System.err.println("TransformerException");
            System.err.println(e);
        }
    }

    public void transform(String xml, String xslt, String rdf)
            throws TransformerConfigurationException,
            TransformerException
    {

        TransformerFactory factory = TransformerFactory.newInstance();
        StreamSource xslStream = new StreamSource(xslt);
        Transformer transformer = factory.newTransformer(xslStream);
        StreamSource in = new StreamSource(xml);
        StreamResult out = new StreamResult(rdf);
        transformer.transform(in, out);
        System.out.println("The generated RDF file is:" + rdf);
    }
}