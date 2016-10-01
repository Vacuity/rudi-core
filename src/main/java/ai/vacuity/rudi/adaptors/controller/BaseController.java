package ai.vacuity.rudi.adaptors.controller;

import java.io.IOException;
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
	public String welcome(@RequestParam("q") String query, ModelMap model) {
		UUID userId = UUID.randomUUID();
		UUID channelId = UUID.randomUUID();
		IRI user = SparqlHAO.getValueFactory().createIRI(Constants.NS_VI + "smonroe");
		IRI channel = SparqlHAO.getValueFactory().createIRI(Constants.NS_VI + "c-" + channelId);
		IndexableInput ii = new IndexableInput(user, channel, query);

		try {
			DispatchService.dispatch(ii);
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

		return welcomeName("<a href=\"http://localhost:8080/rudi-adaptors/a/youtube/youtube-api-results.rdf\">API Response</a>", model);

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
		model.addAttribute("counter", ++counter);
		logger.debug("[welcomeName] counter : {}", counter);
		return VIEW_INDEX;

	}

}