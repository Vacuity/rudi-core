package ai.vacuity.rudi.adaptors.hal.service;

import java.io.IOException;

import ai.vacuity.rudi.adaptors.bo.InputProtocol;
import ai.vacuity.rudi.adaptors.hal.hao.AbstractHAO;
import ai.vacuity.rudi.adaptors.hal.hao.RestfulHAO;
import ai.vacuity.rudi.adaptors.hal.hao.SparqlHAO;
import ai.vacuity.rudi.adaptors.interfaces.impl.AbstractTemplateProcessor;

public class DispatchService {

	public static void dispatch(String input) throws IOException, IllegalArgumentException {
		find_matches: for (InputProtocol ip : SparqlHAO.getInputs()) {
			if (ip == null) break;
			if (ip.hasSparqlQuery()) continue; // don't match alerts
			String call = ip.getResponseProtocol().getCall();
			String log = ip.getResponseProtocol().getLog();
			AbstractHAO hao = null;

			if (ip.getResponseProtocol().hasSparqlQuery()) {
				call = ip.getResponseProtocol().getSparql();
				call = AbstractTemplateProcessor.process(ip, input, call);
				hao = new SparqlHAO();
			}
			else {
				call = AbstractTemplateProcessor.process(ip, input, call);
				hao = new RestfulHAO();
			}
			if (call == null) continue find_matches;
			log = AbstractTemplateProcessor.process(ip, input, log);

			SparqlHAO.logger.debug("[Rudi]: " + log);

			// if (call.indexOf("?") > 0) {
			// String path = call;
			// String params = "";
			// path = call.substring(0, call.indexOf("?"));
			// params = URLEncoder.encode(call.substring(call.indexOf("?") + 1), java.nio.charset.StandardCharsets.UTF_8.toString());
			// call = path + "?" + params;
			// }
			hao.setCall(call);
			hao.setInputProtocol(ip);
			hao.setInput(input);
			hao.run();
		}
	}

	public static void dispatch(int id) throws IOException, IllegalArgumentException {
		find_matches: for (InputProtocol ip : SparqlHAO.getInputs()) {
			if (ip == null) break;
			if (!ip.hasSparqlQuery()) continue; // only match alerts
			String call = ip.getResponseProtocol().getCall();
			String log = ip.getResponseProtocol().getLog();
			AbstractHAO hao = null;

			if (ip.getResponseProtocol().hasSparqlQuery()) {
				call = ip.getResponseProtocol().getSparql();
				call = AbstractTemplateProcessor.process(ip, id, call);
				hao = new SparqlHAO();
			}
			else {
				call = AbstractTemplateProcessor.process(ip, id, call);
				hao = new RestfulHAO();
			}
			if (call == null) continue find_matches;
			log = AbstractTemplateProcessor.process(ip, id, log);

			SparqlHAO.logger.debug("[Rudi]: " + log);

			// if (call.indexOf("?") > 0) {
			// String path = call;
			// String params = "";
			// path = call.substring(0, call.indexOf("?"));
			// params = URLEncoder.encode(call.substring(call.indexOf("?") + 1), java.nio.charset.StandardCharsets.UTF_8.toString());
			// call = path + "?" + params;
			// }
			hao.setCall(call);
			hao.setInputProtocol(ip);
			hao.setInput(ip.getQuery().getLabel()); // send a label form of the query to the response pipeline
			hao.run();
		}
	}

}
