# Getting Started

    1. Edit the /src/main/resources/api.config file
    2. Set the rudi.repo.{via,alerts,responses} properties to point to read/write SPARQL endpoints
    3. Add some API properties, e.g.:
    	example.url=api.example.com/profiles
    	example.id=your API consumer id
    	example.key=your API key
    	example.token=your API secret
    4. Add a listener for an API, see /src/main/webapp/WEB-INF/resources/listeners for examples	(details below)
    5. Run mvn package
    6. Deploy the rudi-adaptors.war file to a web application container
    7. Start the web application container (assuming it's listening on port 8080)
    7. Visit localhost:8080/rudi-adaptors?q=enter+some+keywords

## RDF Universal Distributed Index (RUDI) Overview

RUDI is both a distributed index of event listeners and handlers, and an event relay, or router. The event enters the disparate index via a RUDI node and is routed through the index in the form of a *packet* (a body and a header). Event handlers take the form of web service endpoints and communication endpoints (e.g. a mail inbox). The process of identifying which events an endpoint can handle is called dispatching. When the endpoint is a RUDI peer, the dispatch is called routing. Each router collects and indexes endpoint metadata, and uses this information to dispatch events to appropriate handlers. Response channels within the index are then subscibed to by the agents who trigger the events. Each response channel corresponds to a unique event identifier which is given to the agent at the time RUDI is invoked.

At its core, RUDI is a platform for collecting and federating linked data from dynamic sources (e.g. web service APIs) in response to open ended user queries. The introduction of API replies solicits the development of specialized adaptors and provisioning/maintanance of credentials for each API. One approach is to crowdsource the adaptor development and maintain the API credentials in a centralized store. This index would in turn collect the adaptor descriptions. A better approach is to decentralize the index of adaptors and their associated credentials, and route the user's query to the node housing the associated handler. Event routing is achieved when a RUDI instance subscribes to another RUDI. 

## Listeners

A listener is a set of RDF statements that map an event to a handler. Events are of two types: free-form and semantic. Free-form events are described using `<via:pattern>`. Semantic events are described using `<via:sparql>`. Each event captures data in capture groups. Capture groups are ordered by number and can be labeled using the `<via:label_*>` property. Free-form events use Regex capture groups, or typed patterns. Queries use SPARQL projection items to capture groups. Unlike free form events, query events trigger a series of "result" events, each of which is dispatched to the `<via:notify>`.

Packet routing conforms to a special protocol. More specifically, RUDI generates a header which is appended to the event prior to dispatch in the case of a relay. A ephermal listener is added to the index upon receipt of a packet and is purged from the index when the event's dispatch cycle (at that peer) is complete. Below are some examples of listener descriptions:

```xml

	<!-- 
		Allows an EventHandler to listen to the input channel.
	  -->
    <via:Listener rdf:about="#youtube-url-adaptor">
 		<rdfs:comment>Pulls video URL from youtube.com</rdfs:comment>
 		<rdfs:label>Youtube Request Listener</rdfs:label>
 		<via:event rdf:resource="#youtube_cmd"/>
		<via:notify rdf:resource="#youtube-fetch-videos"/>
 	</via:Listener>

 	<via:EventHandler rdf:about="#youtube-fetch-videos">
 		<via:config>youtube.data</via:config>
 		<via:json rdf:resource="https://www.googleapis.com/youtube/v3/search?part=snippet&amp;type=video&amp;q=${1}&amp;key=${key}"/>
		<via:translator rdf:resource="http://localhost:8080/rudi-adaptors/a/youtube/youtube-api-results.xsl"/>
 		<via:log>Checking youtube.com for videos about '${1}'</via:log>
 		<!-- be sure to add a timestamp before registering the log -->
 	</via:EventHandler>
 	
 	<via:Input rdf:about="#youtube_cmd">
 		<via:pattern rdf:datatype="&via;Regex">^youtube\s*(.*)</via:pattern>
 		<via:label_1>Youtube Search Terms</via:label_1>
 	</via:Input>

 	<via:Listener rdf:about="#listening-adaptor">
 		<rdfs:comment>SPARQL Listener example</rdfs:comment>
 		<rdfs:label>SPARQL Listener example</rdfs:label>
 		<via:event rdf:resource="#get_videos"/>
		<via:notify rdf:resource="#moz-get-backlink-stats"/>
 	</via:Listener>
	
	<via:TupleQuery rdf:about="#get_videos">
		<rdfs:label>get videos</rdfs:label>
 		<via:sparql>
<![CDATA[
	PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
	select ?1 where {
		?1 rdf:type <http://www.w3.org/ns/ma-ont/VideoTrack> .
	}
]]>
		</via:sparql>
 		<via:label_1>Listen for new Youtube vids submitted to the Index</via:label_1>
</via:TupleQuery>

<!-- 
   Allows a SPARQL query to listen to the response channel. 
  -->
 	<via:Listener rdf:about="#test-query-listener">
 		<rdfs:comment>SPARQL Listener example</rdfs:comment>
 		<rdfs:label>SPARQL Listener example</rdfs:label>
 		<via:event rdf:resource="#get_videos"/>
		<via:notify rdf:resource="#virtuoso-sponger"/>
 	</via:Listener>
	
	<via:TupleQuery rdf:about="#get_videos">
		<rdfs:label>get videos</rdfs:label>
 		<via:sparql>
<![CDATA[
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX dc: <http://purl.org/dc/elements/1.1/>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
select ?0 ?1 ?2 ?context where {
	graph ?context {
		?0 rdf:type <http://www.w3.org/ns/ma-ont/VideoTrack> .
		?0 dc:title ?1 .
		?0 dc:description ?2 .
	}
}
]]>
		</via:sparql>
 		<via:label_1>Listen for new Youtube vids submitted to the Index</via:label_1>
	</via:TupleQuery> 	

 	<via:Listener rdf:about="#test-query-listener-2">
 		<rdfs:comment>SPARQL Listener example (send email)</rdfs:comment>
 		<rdfs:label>SPARQL Listener example (send email)</rdfs:label>
 		<via:event rdf:resource="#get_videos-hold"/>
		<via:notify rdf:resource="#smonroe"/>
 	</via:Listener>
```

## Placeholders

EventHandlers describe responses that are triggered by an event. The handler's hyperdata is a template that may contain placeholders. These placeholders are swapped for their values just before the event is dispatched to the `<via:notify>`. The `${number}` placeholder, where "number" is an integer value, is reserved and is swapped for the groups captured by the event. The placeholders `${id}`, `${key}`, `${url}`, and `${token}` are also reserved and are swapped for the config values associated with the `<via:config>` API identity string. The API configuration settings are located in the api.config file.

Custom placeholders may be used, and are swapped by custom TemplateProcessors, which are Java plugins that are called to process the EventHandler descriptions just prior to dispatch. See the JavaDoc for details.

## Channels

**Channels** are virtual communications conduits within RUDI. The two primary types of channels are input and response. The packets these channels carry are comprised of two parts:

1. An RDF URI whose subject is:
   a. A graph SPARQL query that registers on the (structured) input channel, or
   b. Plain-text that registers on the (free-form) input channel. Bots and web services listen for keywords on this channel.
2. Descriptions linking the packet to the Public Key of the originator, optional Public Key of the intended receipient, and other information. Descriptions using the Via ontology constitutes the packet header. Here are some of the packet header properties:
   1. Dispatch log - A cumulative list of endpoints to which the event has been dispatched, from the time of the first invocation up to the current node, and by whom. Each router checks their dispatch targets against this list of endpoints to avoid duplicating replies, then appends its dispatches to the log before routing the event.
   2. Time to live (TTL) - Since RUDI peers must subscribe to downstream routers for replies to their packets, the index will swell with relay hyperdata and in some cases a packet could circulate endlessly. The TTL is a counter which each RUDI peer decrements. The peer which decrements the counter to 0 will dispatch the event but will not route the packet.
   3. Altcoin address - The altcoin type and address for events dispatched to ad servers.

## Sponsors

Events may be handled by an ad server which is configured by the RUDI peer. Once configured, EventHandlers which dispatch to the ad server may be registered with RUDI. The replies from ad servers are transmitted back to the event originator along the Sponsor channel.