package ai.vacuity.rudi.adaptors.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.UUID;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import ai.vacuity.rudi.adaptors.bo.Config;
import ai.vacuity.rudi.adaptors.bo.IndexableInput;
import ai.vacuity.rudi.adaptors.hal.hao.Constants;
import ai.vacuity.rudi.adaptors.hal.hao.GraphManager;
import ai.vacuity.rudi.adaptors.hal.service.DispatchService;
import ai.vacuity.rudi.adaptors.types.Channel;
import ai.vacuity.rudi.adaptors.types.Packet;
import ai.vacuity.rudi.adaptors.types.Response;

@Controller
public class BaseController {

	private static int counter = 0;
	private static final String VIEW_RESULTS = "results";
	private static final String VIEW_LOOKUP = "lookup";
	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(BaseController.class);

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String get(@RequestParam(value = "q", required = false) String q, ModelMap model) {
		if (q == null) q = "";
		model.addAttribute("query", q);
		return VIEW_LOOKUP;
	}

	@RequestMapping(value = "/json", method = RequestMethod.GET)
	public @ResponseBody Response get(@RequestParam(value = "q", required = false) String q) {
		// logger.debug("Reached the controller.");
		try {
			Response r = new Response();

			UUID channelId = UUID.randomUUID();
			IRI user = GraphManager.getValueFactory().createIRI(Constants.NS_VI + "anon");
			IRI channel = GraphManager.getValueFactory().createIRI(Constants.NS_VI + "c-" + channelId);
			long start = System.currentTimeMillis();
			IndexableInput input = new IndexableInput(user, channel, q);
			r.setLogs(DispatchService.dispatch(input));
			long end = System.currentTimeMillis();
			String describeChannel = String.format("select * from named <%s> where {graph <%s>{?s ?p ?o .}}", channel.stringValue(), channel.stringValue());
			// String link = String.format("%s/query?action=exec&queryLn=SPARQL&query=%s&limit_query=100&infer=true&", Constants.SPARQL_ENDPOINT_RESPONSES.replace("rdf4j-server", "rdf4j-workbench"), URLEncoder.encode(describeChannel));

			String link = String.format("%s/explore?resource=<%s>", Config.SPARQL_ENDPOINT_RESPONSES.replace("rdf4j-server", "rdf4j-workbench"), URLEncoder.encode(channel.stringValue()));
			if (Config.SPARQL_ENDPOINT_RESPONSES.indexOf("rdf4j-server") > 0) link = link.replace(Config.SPARQL_ENDPOINT_RESPONSES.substring(0, Config.SPARQL_ENDPOINT_RESPONSES.indexOf("/rdf4j-server")), Config.getRudiContainer().toURL().toString());
			// model.addAttribute("channel", channel.stringValue());
			// model.addAttribute("link", link);
			// model.addAttribute("test", "reached the controller");
			DecimalFormat df = new DecimalFormat("#.###");
			r.setDuration(df.format(new Float((end - start) / 1000f).doubleValue()));
			r.setLink(link);
			r.setChannelId(channel.stringValue());
			r.setMsg("Ok, I'm listening.");
			return r;
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return null;
		// return welcomeName("<h3>Channel ID: <a href=\"" + channel.stringValue() + "\"/>" + channel.stringValue() + "</a></h3><br/>Explore the <a href=\"" + link + "\">Index</a> for data linked to your channel id.", model);

		// logger.debug("[welcome] counter : {}", counter);
		//
		// // Spring uses InternalResourceViewResolver and return back index.jsp
		// return VIEW_RESULTS;

	}

	// @RequestMapping(value = "/{name}", method = RequestMethod.GET)
	// public String welcomeName(@PathVariable String name, ModelMap model) {
	//
	// model.addAttribute("test", name);
	// // model.addAttribute("counter", ++counter);
	// // logger.debug("[welcomeName] counter : {}", counter);
	// return VIEW_RESULTS;
	//
	// }

	public void main(String[] args) {
		RestTemplate restTemplate = new RestTemplate();
		String peer = "http://localhost:8080/spring-security-rest-full/foos";

		// fetch raw response
		ResponseEntity<String> response = restTemplate.getForEntity(peer + "/1", String.class);
		// assertThat(response.getStatusCode(), is(HttpStatus.OK));

		// fetch object from response stream
		HttpEntity<Packet> request = new HttpEntity<>(new Packet());
		Channel reply = restTemplate.postForObject(peer, request, Channel.class);
	}

}