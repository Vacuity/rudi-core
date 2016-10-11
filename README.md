# Getting Started

## Requirements

- [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
- [Java Servlet Container](https://en.wikipedia.org/wiki/Web_container#List_of_Servlet_containers)
- Read/Write access to a [SPARQL Endpoint](https://en.wikipedia.org/wiki/List_of_SPARQL_implementations)

## How to Install

1. Edit the `./src/main/resources/api.config` file
2. Set the `rudi.repo.{via,alerts,responses}` properties to point to read/write SPARQL endpoints
3. Add some API properties, e.g.:
   `example.url=api.example.com/profiles`
   `example.id=your API consumer id`
   `example.key=your API key`
   `example.token=your API secret`
   4. Add a listener for an API, see `./src/main/webapp/WEB-INF/resources/listeners` for examples(details below)
4. Build `rudi-adaptors.war` (see below)
5. Deploy the `rudi-adaptors.war` file to a web application container
6. Start the web application container (assuming it's listening on port 8080)
7. Visit localhost:8080/rudi-adaptors?q=some+keywords

### How to Build RUDI
1. Install [Maven](http://maven.apache.org/download.cgi)
2. Install [Git](https://git-scm.com/downloads)
3. Run `git clone https://github.com/Vacuity/rudi-core`
4. Download the [virtjdbc4.jar](http://opldownload.s3.amazonaws.com/uda/virtuoso/7.2/jdbc/virtjdbc4.jar) and [virt_rdf4j.jar](https://github.com/sdmonroe/virtuoso-opensource/blob/develop/7/binsrc/rdf4j/virt_rdf4.jar) jars, and add them to your local Maven repo with the following commands:
   `mvn install:install-file -Dfile=<path-to-virtjdbc.jar> -DgroupId=virtuoso.rdf4j -DartifactId=virtuoso-jdbc4 -Dversion=4 -Dpackaging=jar -DgeneratePom=true`
   `mvn install:install-file -Dfile=<path-to-virt_rdf4j.jar> -DgroupId=virtuoso.rdf4j -DartifactId=virtuoso-rdf4j -Dversion=4 -Dpackaging=jar -DgeneratePom=true`
5. `cd` to the root of the rudi-adaptors project directory and run `mvn clean install`

## RDF Universal Distributed Index (RUDI) Overview

RUDI is a distributed index of event listeners and handlers coupled with an event relay, or router. The event enters the disparate index via a RUDI node and is routed through the index in the form of a *packet* (a body and a header). Event handlers capture data from the event which is then passed as parameters to web services or other communication endpoints (e.g. a mail inbox). The process of identifying which events an endpoint can handle is called dispatching. When the endpoint is a RUDI peer, the dispatch is referred to as routing:

![RUDI sequence diagram](https://docs.google.com/drawings/d/14kma-KSoN8SgVgZfJqqoiRnvB24INC3bHRP9m5T3yIw/pub?w=1380&h=919)  

Routers are implemented as nodes in a [Pastry overlay network](http://www.freepastry.org/). Each router collects and indexes endpoint hyperdata, and uses this information to dispatch events to appropriate handlers. Response channels within the index are then subscibed to by the agents who trigger the events. Each response channel corresponds to a unique event identifier which is given to the agent at the time RUDI is invoked.

At its core, RUDI is a platform for marshalling and federating linked data from dynamic sources (e.g. web service APIs) in response to open ended user queries. This is trivial with [RDF and SPARQL](https://www.informatik.hu-berlin.de/de/forschung/gebiete/wbi/research/publications/2008/DARQ-FINAL.pdf). The introduction of heterogenous APIs as data source endpoints solicits the development of specialized adaptors as well as the provisioning/maintanance of credentials for each API. One approach is to crowdsource the adaptor development and maintain the API credentials in a centralized store. This index would in turn be the central collector and consumer of adaptor descriptions. A better approach is to decentralize the index of adaptors and their associated credentials, and route the user's query to the node housing the appropriate handler. Event routing is achieved by broadcast, or when a RUDI instance subscribes to another RUDI. Members of a peer group simply pass their queries along to their peers in addition to dispatching the queries. A node selectively tunes in to peer broadcasts and dispatches the events which it can handle. The response is then published to the origin via an ephemeral `<via:Listener>` wherein the origin is the `<via:notify>`.

## Routing and Caching
A RUDI query is broadcasted to its list of peers, and peers send their requests to their list of peers, and so on, in addition to dispatching the request to any web service associated with matched listeners. Web service replies (that result from listener matches at a certain node) are directed to the origin peer. The nodes also send the origin the regex pattern they matched. This pattern is cached by the recipient node and mapped to the responder node. On every input, the cache is checked before RUDI broadcasts the request to its peers. If a match is found in the cache, that node is used instead of the broadcast. Broadcasts should be a last resort to reduce network chatter. The goal minimized broadcasts by having queries sent directly to the nodes that can handle it as often as possible. The broadcast is chiefly an avenue to build up a good cache of peers at each node. The cache should therefore reflect the interests of the node's end-users.

## Regex Ranking
The dispatch algorithm rewards event patterns based on *specificity*. The intuition is that the more specific the pattern, the more semantics are understood by it, and thus, the more appropriate the response of its event handler. So, the pattern `^youtube .*` will be ranked higher than `^.*$`. Since a node is designed to rely heavily on its local cache of mapped regexes, authors will need to work at making the regex's they publish very specific. Otherwise they will be outranked in a nodes' cache. A published list of ranking rules is provided to help authors. For example, the current algorithm removes `[]`-groups and the ranking = the length of the residual regex. A `^` at the start of the pattern gets a point boost, as does a `$` at the end of a pattern. Wildcards get points deducted, and so on. This approach requires linear time matching and therefore does not scale. A matcher [similar to setfiles in SELinux](https://www.csee.umbc.edu/wp-content/uploads/2014/03/Miner-Pairing-Strings-RegExps.pdf) is now being investigated. The algorithm uses a graph-based matching approach to achieve matches in constant polynomial time.

## Sensors

A sensor monitors a 'physical channel' for events. The current implementation supports HTTPSensor (web traffic) and OverlaySensor (P2P traffic).

## Listeners

A listener is a set of RDF statements that map an event to a handler. Events are of two types: free-form and semantic. Free-form events are described using `<via:pattern>`. Semantic events are described using `<via:sparql>`. Each event captures data in capture groups. Capture groups are ordered by number and can be labeled using the `<via:label_*>` property. Free-form events use Regex capture groups, or typed patterns. Queries use SPARQL projection items to capture groups. Unlike free form events, query events trigger a series of "result" events, each of which is dispatched to the `<via:notify>`.

Packet routing conforms to a special protocol. More specifically, RUDI generates a header which is appended to the event prior to dispatch in the case of a relay. An ephemeral listener is added to the index upon receipt of a packet and is purged from the index when the event's dispatch cycle (at that peer) is complete. Below are some examples of listener descriptions:

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
 		<via:labels>,Youtube Search Terms</via:labels>
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
 		<via:labels>,Listen for new Youtube vids submitted to the Index</via:labels>
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
   1. Dispatch log - A cumulative list of endpoints to which the event has been dispatched, from the time of the first invocation up to the current node, and by whom. Each router checks their dispatch targets against this list of called endpoints to avoid duplicating replies, then appends its invocations to the log before dispatching the event.  Each peer also checks its peers list against the list of previous dispatchers before relaying the packet.
   2. Time to live (TTL) - Since RUDI peers must subscribe to downstream routers for replies to their packets, the index will swell with relay hyperdata and in some cases a packet could circulate endlessly. The TTL is a counter which each RUDI peer decrements. The peer which decrements the counter to 0 will dispatch the event but will not route the packet.
   3. Altcoin address - The altcoin type and address for events dispatched to sponsor nodes.

Channels are RESTfully navigated (the user requests some data to start with, then browses the results and requests the "rest" as needed). To enable this mode of browsing, service responses are converted to hyperdata (the resources are linked to resources contained elsewhere in the great global graph).

## Sponsors

Events may be handled by a sponsor node which is configured by the RUDI peer. Once configured, EventHandlers which dispatch to the sponsor node may be registered with RUDI. A sponsor node collects listeners whose EventHandlers describe HTML5 content instead of endpoint invocations. Replies from sponsor nodes are transmitted back to the event originator along the Sponsor channel.

## Listener Specification

| Tag                  | Description                              |         Required         | Cardinality |
| -------------------- | :--------------------------------------- | :----------------------: | :---------: |
| **via:Listener**     | A RUDI listener                          |                          |      n      |
| - rdfs:comment       | brief documentation                      |          false           |      n      |
| - rdf:label          | human-readable name                      |          false           |      n      |
| - via:event          | event to monitor; can be of type `via:Input` or `via:Query` |           true           |      n      |
| - via:notify         | A `via:EventHandler`, the URI of an entity with `via:cep` (communication endpoint) properties, such as a [LDN URI](https://www.w3.org/TR/2016/WD-ldn-20160726/), a SMS phone number, or an email inbox, to which the `via:event` is passed |           true           |      n      |
| **via:EventHandler** | accepts an event to disptach; the captured data is passed to via:call values via ${index} placeholders, where 'index' is the index of the captured datum |                          |             |
| - via:config         | the config in the settings.ini file which registers the custom `${tags}` to be used in this handler, and the endpoint attributes by which the via:call properties are sandboxed (the domain, port, and protocol assigned by the `via:config` must match thosed used in the `via:call` or a Security violation is thrown) |           true           |      1      |
| - via:json           | a `via:call` property whose value is the web service call to which the captured data is dispatched, and which produces JSON; the JSON is RDFized and stored in the Index |          false           |      n      |
| - via:rdf-xml        | a `via:call` property whose value is the web service call to which the captured data is dispatched, and which produces RDF-XML; the RDF is stored in the Index |          false           |      n      |
| - via:translator     | the URL of an XSLT sheet which translates the XMLized output of a via:json call into RDF+XML. The http://rudi.TAG.placeholders.vacuity.ai is a placeholder URL prefix which is substituted by RUDI for the URL path configured by the `rudi.TAG` property in settings.ini. The property can, for example, point to a resource path on the RUDI host. The path property must be absolute. Trust of the via:translator URL is provided by `via:signature`. | when via:json is present |      1      |
| - via:query          | the `via:Query` to which events are passed, the result set is RDFized and added to the Index |          false           |      n      |
| - via:log            | logs the dispatch                        |          false           |      n      |
| **via:signature**    | the [XML Signature](https://web-payments.org/vocabs/signature#XmlSignature) of the resource described by this property; the `rudi.translators.validate` and `rudi.call.validate` boolean properties in settings.ini control whether via:call and via:translator values are validated (***[TODO](http://docs.oracle.com/javase/7/docs/technotes/guides/security/xmldsig/XMLDigitalSignature.html)***). The Index will need to contain a nix:Trust idea linking the RUDI endpoint to the signer, and the idea needs to be signed by the RUDI endpoint. |          false           |             |
| **via:Input**        | captures events at RUDI's sensors, e.g. the web service controller |                          |             |
| - via:pattern        | captures data from an event pattern of type `rdf:datatype`. The `via:Input` is a `rdfs:subClass` of `rdf:List` and `via:pattern` is a `rdfs:subProperty` of `rdf:first`. The `rdf:first`- ness of `via:pattern` is merely a cosmetic feature. Since some quad store frameworks may not support inferring sub properties of `rdf:first`, it is safer to use `rdf:first` explicily. The list items describe a hybrid pattern comprised of patterns of various `rdf:datatype`. The `via:pattern` specifes a *typed pattern* if its `rdf:datatype` property is a value other than `via:Regex`. For type `xsd:dateTime`, the `via:pattern` value specifies the date format. All other non-regex patterns have sample values which conform to the `rdf:datatype`. A list of supported typed patterns are provided in `rudi-adaptor.rdf`. |           true           |      n      |
| - via:labels         | a comma-delimited list, each item labels the datum at the corresponding index in the event pattern. Escape comma using '\,' |          false           |      n      |
| **via:Query**        | captures update events occuring in the Index; types are rdf:TupleQuery, rdf:GraphQuery, and rdf:BooleanQuery |                          |             |
| - via:sparql         | the sparql query whose projection captures data from the update event. The projection element names are numbers corresponding to indecies referenced in the Event Handler. The projection item name may be a number followed by the string \_label, for example ?1\_email. Each query result triggers the Event Handler. |           true           |      n      |
| - via:labels         | comma-delimited list of labels of data captured by numbered projection elements |          false           |      n      |

## Index Graph Structure

The Index is designed to be RESTfully navigated. Here is a diagram of the Index graph's basic structure, followed by a description of the entity types:

![RIGS](https://docs.google.com/drawings/d/1vOwOWbUxJytdbS8pVr21jkUnWY1vhGxi584WGTt9x-I/pub?w=1724&h=1052)

* **User** - the IRI of the user who originated an event at a RUDI sensor
* **Event Handler** - the IRI value of a via:notify
* **via:Channel** - an IRI representing an event detected by RUDI at one of its sensors
* **via:Response** - the IRI of the response generated by a via:call or via:query
* **via:Communication_response** - the native idea model of the via:Response
* **via:QueryResult** - a single result in the result set of a via:query
* **via:Hit** - a single result in the result set of a via:event of type via:Query
* **via:Projection** - the projection variable/value binding contained in a via:QueryResult or via:Hit
* **via:Alert** - the IRI of a stimulating Index update event scoped to a via:notify that ranges over a via:Query, and where the update event spans a document insert procedure, insertion of a list of statements, or insertion of a single statement. The via:notify should use the ${context} to restrict the results of the query to the context IRI provided by the update event, since this context is always the IRI of the via: