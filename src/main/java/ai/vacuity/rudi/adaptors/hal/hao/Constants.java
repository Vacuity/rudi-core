package ai.vacuity.rudi.adaptors.hal.hao;

import java.io.File;

import ai.vacuity.rudi.adaptors.bo.Config;

public class Constants {

	/*
	 * 
	 * xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:contact="http://www.w3.org/2000/10/swap/pim/contact#" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:h="http://www.w3.org/1999/xhtml" xmlns:ep="http://www.snee.com/ns/ep">
	 */

	public final static String NS_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public final static String NS_FOAF = "http://xmlns.com/foaf/0.1/";
	public final static String NS_CONTACT = "http://www.w3.org/2000/10/swap/pim/contact#";
	public final static String NS_DC = "http://purl.org/dc/elements/1.1/";
	public final static String NS_TERMS = "http://purl.org/dc/terms/";
	public final static String NS_QANVAS = "http://ns.qanvas.org/";
	public final static String NS_GR = "http://purl.org/goodrelations/v1#";
	public final static String NS_XSD = "http://www.w3.org/2001/XMLSchema#";
	public final static String NS_RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	public final static String NS_DCTERMS = "http://purl.org/dc/terms/";
	public final static String NS_DBPROP = "http://dbpedia.org/property/";
	public final static String NS_DBPEDIA = "http://dbpedia.org/ontology/";
	public final static String NS_SIOC = "http://rdfs.org/sioc/ns#";
	public final static String NS_NEPOMUK = "http://www.semanticdesktop.org/ontologies/2007/03/22/nmo/";
	public final static String NS_MOAT = "http://moat-project.org/ns#";
	public final static String NS_OAT = "http://www.w3.org/2002/07/owl#";
	public final static String NS_SKOS = "http://www.w3.org/2004/02/skos/core#";
	public final static String NS_WN2 = "http://www.w3.org/2006/03/wn/wn20/schema/";
	public final static String NS_WN3 = "http://purl.org/vocabularies/princeton/wn30/";
	public final static String NS_NIX = "http://www.vacuity.ai/onto/nix/1.0/";
	public final static String NS_VIA = "http://www.vacuity.ai/onto/via/1.0/";
	public final static String NS_MA_ONT = "http://www.w3.org/ns/ma-ont/";
	public final static String NS_OWL = "http://www.w3.org/2002/07/owl#";
	public final static String NS_VI = "http://www.vacuity.ai/onto/instances/1.0#";
	public final static String NS_FREE = "http://freebase.com/";
	public static final String CONTEXT_DEMO = "http://tryrudi.io/rdf/demo/";
	public static final String CONTEXT_VIA = "http://tryrudi.io/rdf/via/";
	public static final String DIR_LISTENERS = (Config.getSettings().containsKey("rudi.data")) ? Config.getSettings().getProperty("rudi.data") + "listeners/" : System.getProperty("user.home") + File.separator + "listeners/";
	public static final String DIR_RESPONSES = (Config.getSettings().containsKey("rudi.data")) ? Config.getSettings().getProperty("rudi.data") + "responses/" : System.getProperty("user.home") + File.separator + "responses/";
	public static final String DIR_ALERTS = (Config.getSettings().containsKey("rudi.data")) ? Config.getSettings().getProperty("rudi.data") + "data/" : System.getProperty("user.home") + File.separator + "alerts/";

	public static final String SPARQL_ENDPOINT_VIA = Config.getSettings().getProperty("rudi.repo.via");
	public static final String SPARQL_ENDPOINT_ALERTS = Config.getSettings().getProperty("rudi.repo.alerts");
	public static final String SPARQL_ENDPOINT_RESPONSES = Config.getSettings().getProperty("rudi.repo.responses");

	public static final String RUDI_DOMAIN = Config.getSettings().getProperty("rudi.domain");

}
