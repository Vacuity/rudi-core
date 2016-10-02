package ai.vacuity.rudi.adaptors.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.UUID;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ai.vacuity.rudi.adaptors.bo.IndexableInput;
import ai.vacuity.rudi.adaptors.hal.hao.Constants;
import ai.vacuity.rudi.adaptors.hal.hao.SparqlHAO;
import ai.vacuity.rudi.adaptors.hal.service.DispatchService;

@Controller
public class BaseController {

	private static int counter = 0;
	private static final String VIEW_INDEX = "index";
	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(BaseController.class);

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String welcome(@RequestParam("q") String q, ModelMap model) {
		UUID channelId = UUID.randomUUID();
		IRI user = SparqlHAO.getValueFactory().createIRI(Constants.NS_VI + "anon");
		IRI channel = SparqlHAO.getValueFactory().createIRI(Constants.NS_VI + "c-" + channelId);
		IndexableInput input = new IndexableInput(user, channel, q);

		try {
			DispatchService.dispatch(input);
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

		// String describeChannel = String.format("select * from named <%s> where {graph <%s>{?s ?p ?o .}}", channel.stringValue(), channel.stringValue());
		// String link = String.format("%s/query?action=exec&queryLn=SPARQL&query=%s&limit_query=100&infer=true&", Constants.SPARQL_ENDPOINT_RESPONSES.replace("rdf4j-server", "rdf4j-workbench"), URLEncoder.encode(describeChannel));

		String link = String.format("%s/explore?resource=<%s>", Constants.SPARQL_ENDPOINT_RESPONSES.replace("rdf4j-server", "rdf4j-workbench"), URLEncoder.encode(channel.stringValue()));

		return welcomeName("<h3>Channel ID: <a href=\"" + channel.stringValue() + "\"/>" + channel.stringValue() + "</a></h3><br/>Explore the <a href=\"" + link + "\">Index</a> for data linked to your channel id.", model);

		// model.addAttribute("message", "Welcome");
		// model.addAttribute("counter", ++counter);
		// logger.debug("[welcome] counter : {}", counter);
		//
		// // Spring uses InternalResourceViewResolver and return back index.jsp
		// return VIEW_INDEX;

	}

	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	public String welcomeName(@PathVariable String name, ModelMap model) {

		model.addAttribute("message", name);
		// model.addAttribute("counter", ++counter);
		// logger.debug("[welcomeName] counter : {}", counter);
		return VIEW_INDEX;

	}

}