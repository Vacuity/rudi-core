# <a name="gs"/>Getting Starting
RUDI runs as a web server and persists its data in an RDF [quad store](http://dbpedia.org/page/Triplestore) (the Index). Below are the instructions for setting up the server.

#### <a name="sr"/>System Requirements

- [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
- [Java Servlet Container](https://en.wikipedia.org/wiki/Web_container#List_of_Servlet_containers)
- Read/Write access to a [SPARQL Endpoint](https://en.wikipedia.org/wiki/List_of_SPARQL_implementations)

#### <a name="hti"/>How to Install

1. Edit the node settings file at `./src/main/resources/settings.ini` (this file is automatically moved to `<user.home>/.rudi/settings.ini` after the first program startup)
2. Set the `rudi.repo.{via,alerts,responses}` properties to point to read/write SPARQL endpoints
3. Add some API properties, e.g.:
   `example.url=api.example.com/profiles`
   `example.id=your API consumer id`
   `example.key=your API key`
   `example.token=your API secret`
4. Add a listener for an API, see `./src/main/webapp/WEB-INF/resources/listeners` for examples (details below)
5. Add some peers, see `settings.ini` for examples
6. Build `rudi-adaptors.war` (instructions below)
7. Deploy the `rudi-adaptors.war` file to a web application container
8. Start the web application container (assuming it's listening on port 8080)
9. Visit localhost:8080/rudi-adaptors?q=some+keywords

#### <a name="hobr"/>How to Build RUDI
1. Install [Maven](http://maven.apache.org/download.cgi)
2. Install [Git](https://git-scm.com/downloads)
3. Run `git clone https://github.com/Vacuity/rudi-core`
4. Download the [virtjdbc4.jar](http://opldownload.s3.amazonaws.com/uda/virtuoso/7.2/jdbc/virtjdbc4.jar), [virt_rdf4j.jar](https://github.com/sdmonroe/virtuoso-opensource/blob/develop/7/binsrc/rdf4j/virt_rdf4.jar), [FreePastry-2.1.jar](http://www.freepastry.org/FreePastry/FreePastry-2.1.jar) jars, and add them to your local Maven repo with the following commands:
```{r, engine=sh, count_lines}
mvn install:install-file -Dfile=<path/to/virtjdbc.jar> -DgroupId=virtuoso.rdf4j -DartifactId=virtuoso-jdbc4 -Dversion=4 -Dpackaging=jar -DgeneratePom=true

mvn install:install-file -Dfile=<path/to/virt_rdf4j.jar> -DgroupId=virtuoso.rdf4j -DartifactId=virtuoso-rdf4j -Dversion=4 -Dpackaging=jar -DgeneratePom=true

mvn install:install-file -Dfile=<path/to/FreePastry-2.1.jar> -DgroupId=rice.pastry -DartifactId=freepastry -Dversion=2.1 -Dpackaging=jar -DgeneratePom=true
```
5. `cd` to the root of the `rudi-core` project directory and run `mvn clean install`
6. The war file is located at `./rudi-core/target/rudi-adaptors.war`

# <a name="contents"/>Contents

[Protocol Description](#protocol)

1. [RUDI Overview](#overview)
2. [Event Routing](#routing)
3. [Topical Broadcasting](#broadcasting)
4. [Dispatch Round](#dispatch)
5. [Distributed Querying](#distroQuery)
6. [Web Service Reply Cache](#cache)

[VI Programming](#programming)

1. [Regex Ranking](#regex)
2. [Sensors](#sensors)
3. [Listeners](#listeners)
4. [Placeholders](#placeholders)
5. [Channels](#channels)
6. [Sponsors](#sponsors)
7. [Listener Specification](#listenerSpec)
8. [Local Graph Structure](#localGraph)
9. [Dialog](#dialog)

#<a name="protocol"/>Protocol Description
This section outlines the guiding principles of the RUDI network and the communication protocol concepts.

##<a name="overview"/>RDF Universal Distributed Index (RUDI) Overview

RUDI is a distributed index of event listeners, handlers and related logic coupled with an event relay network. Events enter the disparate index via a RUDI node and are routed through the index in the form of a *packet* (a body and a header). Event handlers at the origin node capture data from the event and pass the captures as parameters to web services or other communication endpoints (e.g. a mail inbox). The process of identifying which events an endpoint can handle is called dispatching. When the endpoint is a RUDI peer, the dispatch is referred to as routing. The data retrieved from a dispatch is simply known as raw data:

![RUDI sequence diagram](https://docs.google.com/drawings/d/14kma-KSoN8SgVgZfJqqoiRnvB24INC3bHRP9m5T3yIw/pub?w=1380&h=919)  

##<a name="routing"/>Event Routing

Routers are implemented as nodes in a [Pastry overlay network](http://www.freepastry.org/). Each peer collects and indexes endpoint hyperdata, and uses this information to dispatch events to appropriate handlers. Response channels within the index are then subscibed to by the agents who trigger the events. Each response channel corresponds to a unique event identifier (the channel id) which is given to the agent at the time RUDI is invoked. An event captured by a RUDI node is broadcasted to a list of subscribers in addition to being dispatched to any web services associated with matched listeners. Web service emissions are directed to the origin peer. The nodes also send the origin the regex pattern which matched. This pattern is cached by the recipient node and mapped to the responder node. On every input, the cache is checked before RUDI broadcasts the request. If a match is found in the cache, that node is used instead of the broadcast. Broadcasts are treated as a last resort procedure to reduce network chatter. The goal is to minimize broadcasts by having queries sent directly to the most appropriate nodes as often as possible. The broadcast is chiefly an avenue to build up a good cache of peers at each node. The cache should therefore increasingly reflect the interests of the node's end-users.

At its core, RUDI is a platform for marshalling and federating linked data from dynamic sources (e.g. web service APIs) in response to open ended user queries. This is trivial with [RDF and SPARQL](https://www.informatik.hu-berlin.de/de/forschung/gebiete/wbi/research/publications/2008/DARQ-FINAL.pdf). The introduction of heterogenous APIs as data source endpoints solicits the development of specialized adaptors as well as the provisioning/maintanance of credentials for each API. One approach is to crowdsource the adaptor development and maintain the API credentials in a centralized store. This index would in turn be the central collector and consumer of adaptor descriptions. A better approach is to decentralize the index of adaptors and their associated credentials, and route the user's query to the node housing the appropriate handler. Event routing is achieved via topical broadcast.

##<a name="broadcasting"/>Topical Broadcasting

RUDI features a [SCRIBE](http://www.freepastry.org/PAST/anycast.pdf) message board to which events are broadcast under *topic* IRIs. Topic subscribers  reply with useful behavior in response to posts. In the broadcast procedure, the event is optionally published to the `via:GetTopic` topic prior to routing. This topic is subscribed to by RUDI peers who possess listeners linked to `via:TopicProvider`, which is a `rdf:subClass` of `via:EventHandler` and which emits topics for event patterns. The topic server replies to origin with a list of ranked topics. The origin node then broadcasts the event to the list of topics it received, in accordance with pre-defined selection criteria managed by the node owner. Otherwise, the event is broadcast under topics provided by the origin node itself. In either case, different contextual constraints accompanying the event (a faceted query view element, an LDPath, a #hashtag, etc.) are used to generate the topic. Likewise, nodes who broadcast events to the `via:GetSPARQL` topic will receive the event in SPARQL format, events broadcast to `via:GetReport` will receive the report for data captured from the event as assigned by `via:Listener` (`via:labels`, match score, match duration, etc), and nodes who broadcast a QueryResultSet packet to `via:GetViewlet` will receive an HTML5 representation of the result set. Typical subscribers simply dispatch the event and relay the raw data to origin.

A user may register a topic which they alone can access. This is done by 

##<a name="dispatch"/>Dispatch Round

There is a high likelihood that multiple nodes subscribing to the same topic will dispatch the topic's events to the same endpoint. This is paramount to the origin node resubmitting the same request to the endpoint multiple times. This results in superflous results and abusive web API usage. To avoid this, a [virtual token ring](http://dbpedia.org/page/Token_ring) is formed by the subscribers. More specifically, for every web service endpoint ENPT handled by a node, the node registers to the token ring by sending a PUT message to the ring key.  The key a node uses to retrieve its downstream peer in the token ring is `ENPT + EVENT + topic`.  The PUT message causes the DHT (more specifically, the node that manages the key/value mapping for `ENPT + EVENT + topic`) to add the client node to the ordered list of nodes who dispatch the topic's events to `ENPT` and have subscribed to the key's topic.  The client node then retrieves from the DHT the node following it in ordered list. The node that creates the mapping holds the token initially. It therefore waits several seconds then pulls the list and checks its claim against any added nodes by sending all nodes in the list a message. If its claim is contested, the node whose nodeId is highest in the 128-bit address space wins. Prior to dispatch, each topic subscriber checks whether it possesses the token. If not, then the dispatch is not executed. Once a node dispatches the event it passes the token to its downstream neighbor in the ring, along with the non-sensitive list of parameters and values it passed to the endpoint, and a flag indicated whether the resulting raw data is empty. The receiving node will refrain from dispatching the event unless 1) the raw data from the previous dispatch was empty, and 2) the parameters it received are different from the ones it plans to dispatch. After reaching a decision, it passes the token. The last node in the ring passes the token to the first node, and the round is over. The first node holds the token until the next event under `ENPT + EVENT + topic` enters its queue. The origin node can check response collisions and unauthorized replies by inspecting the DHT for the previous token owner. The key is available to the origin node so long as at least one of the repliers sends the valid `ENPT`. To increase accountability, the token ring key manager can notify the topic key manager of its hosts list, and the origin can retrieve the list of token rings from the topic key manager. The absence of a replier from any of these list can then be checked by origin. The dispatch round is intended to facilitate coordination between repliers who must access a shared resource. The impact of a bad actor is superfluous API calls, which one assumes is undesirable for the replier since most APIs impose call rationing. Blatant spam is addressed with PKI. Nevertheless, the node id, public key, etc of dipatch round violators can then receive a nix from the origin node. These nixes can be stored in the local Index and used by all nodes as a reply filter via distributed querying.

##<a name="distroQuery"/>Distributed Querying

RUDI distributes SPARQL queries only to nodes which emit results for the query. RUDI achieves this by leveraging a service description index simiar to [DARQ](https://www.informatik.hu-berlin.de/de/forschung/gebiete/wbi/research/publications/2008/DARQ-FINAL.pdf), but stores the service descriptions in a distributed hashtable (DHT). In order to evaluate the efficacy of the RUDI network, it is important to understand the design elements of the [Pastry overlay substrate](https://www.cs.rice.edu/~druschel/publications/Pastry.pdf) which implements the DHT and the peer-network routing in general.

### Implementation Details

On each Index update and remove *event*, RUDI normalizes and queues the set of incomming triples, along with the event type, and metadata for each node in the set of triples (e.g. triple slot). Part of normailization is the generating of a generalized model by substituting individuals (or instances, that is, IRI subjects and objects which are not classes) for their `owl:Class` types, the nullification of objects where the subject is an individual IRI, and the removal of any triples that contain a node linked to false by `via:indexable`. A cron process then stores the events in the DHT by mapping each IRI or Literal to that peer's node id. A counter, which increments for each update and decrements for each remove, is associated with each mapped value in the DHT. A [Properties](https://docs.oracle.com/javase/tutorial/essential/environment/properties.html) file P (contained within a directory whose name is  S, P or O) whose name is a hash of the IRI or Literal (the key) and which contains the property `hosts` (the list of host) and `host.prop` where `prop` is a property for host whose value is specfic to the triple node represented by P, encapsulates the map for each entry in the DHT.

To distribute a SPARQL query, RUDI tokenizes the SPARQL into triples to produce a set of IRIs and a set of constraints (e.g. REQUIRED) for each. Let these be variables:

- STS - the set of triples for a SPARQL query, and constraints for each triple
- K - an arbitrary IRI or Literal in STS (the key)
- SRVR - the RUDI node servicing the lookup for K
- H - the RUDI node mapped to K in the DHT

In addition to K, RUDI passes to SRVR the SLOTS (s, p, and/or o) for K, and the set STS - K. Node SRVR fetches the list of values (HOSTS) mapped to K@SLOTS which satisfy the constraints for all triples involving K, selects an arbitrary node H in HOSTS, builds a packet containing MATCHED_HOSTS = MATCHED_HOSTS + H, H → K@SLOTS, and STS = STS - K. Node SRVR then lookups up H in the DHT, passing the packet along with the key, and this continues until the set STS is empty. The MATCHED_HOSTS list is returned to the origin by the last router in the chain. The origin then executes H → K@SLOTS on all values H and collects the results in the graph corresponding to the query's channel id. Finally, STS is executed against the federated graph. Counter values in MATCHED_HOSTS are replaced with those at H if and only if they are lower then those in MATCHED_HOSTS. The MATCHED_HOSTS list is an intersect of all members H where {K | K ∈ STS} → H where H meets all contraints on K defined by STS, and the counters describe the number of results for all K@SLOTS at H. Distributing the computation reduces the search space for the intersect operation. In addition to the counter, additional information may be associated with the elements in H.

When joining the network, a node registers all its content with the DHT.

### Query Optimization

As in DARQ, logical and physical query opimizatizing transformations are performed prior to query dispatch in order to increase response speed and decrease execution costs.

### Namespace Prefixes

An instance of the DHT maps XML namespace qualified names to their IRI prefixes. RUDI routinely retrieves the list of namespaces prefixes it knows about and compares the list to those reported on the previous check. The diff is added to the known prefix list and the new namespaces are stored in the DHT. Each namespace mapping has associated with it the list of nodes who know about the namespace mapping. A remove operation removes from the sources list the node that invoked the remove operation. When all nodes have been removed from the sources list, the namespace mapping is removed from the Index.

##<a name="cache"/>Web Service Reply Cache

All nodes in the network make use of a shared, distributed Web Service Reply Cache similar to [Squirrel](http://www.freepastry.org/PAST/squirrel.pdf).

#<a name="programming"/>VI Programming
The RUDI network replies to events with behavior that is useful to its clients, like performing actions or returning information. This behavior is “smart” in as much as it is influenced by semantics encapsulated by the event. The network harvests intelligence by dismantling its problem set into a "jigsaw puzzle", the solution to which many individuals can contribute small portions, perhaps three or four fitted pieces. These individuals give the portions they solve a unique identifier like “034” and add assembly instructions such as “pieces 034 and 091 fit”. The individual then leaves the pieces scattered and a computer program is later called to solve some portion of the puzzle. Though it is merely doing what programs have always been good at (following opaque, but well-defined, procedures), to an unwitting observer it appears the program has intelligently assembled the jigsaw puzzle. This [ELIZA-style](https://en.wikipedia.org/wiki/ELIZA) approach of leveraging many self-contained, stimulus/response scripts, or microprocedures, is quite effective when it works, but is often plauged by the brittleness barrier. Intelligence is exhibited within the agent's input domain but the domain is easily evaded by the end-user. A certain critical mass of microprocedures is required to overcome this barrier in non-trivial contexts. With the introduction of networking, the intelligence of a large army of individuals can be brought to bear to reach this critical mass, thus overcoming the brittleness barrier. This transfers the problem of machine intelligence from the domain of theory and the province of esoteric algorithms to the well charted domains of protocol design and networking. The logic tier has been evacuated. *How* the nodes are linked has become the intelligence. The algorithm has been [ephemeralized](https://en.wikipedia.org/wiki/Ephemeralization) by a protocol. The network has replaced the role of the logic board. The resulting intelligence is all smoke and mirrors. This is the vacuous model, which facilitates the virtualization of the smart agent by decoupling the intelligence and the agent that consumes and applies it. In such a model the end-user is removed no more than one degree from geniune intellect. The agent is analogous to a puppet serving as a vacuous intermediary for (i.e. a faithful mirror of) its master's behavior. The puppeteer serves as the agent's primary source of intelligence. An agent who relies heavily on a learning set is not vacuous, but would set the end-user at least two degrees from geniune intellect (human generates learning algorithm, learning algorithm generates the intelligence). With RUDI, the network intercedes for *virtual intelligence*, where a node's behavior is analogous to a film comprised of still frames, each of which depicts the performance of some one of an innumerable number of actors.

There is a territory of phenomena whose manifestations are caused by indeterminate factors. This class of events is embraced by the NP-complete set. Problems involving such phenomena have solutions that are not conducive to formal expression (e.g. the event cannot be generated by a grammar *a priori*, and must instead be explicitly enumerated). This limits the production of event recognition logic at the network's sensory layer. For this reason, virtual intelligence is an inescapable simulation of the real thing, there are a set of problems (encapsulated as events) for which the network cannot serve as intermediary between problem-bearer and problem-solver. Something is neccessarily absent in the *imitator* (the network) that is present at its source (the cloud of intellects), and this limitation is inversely proportional to the expressive power of the language encapsulating the sensory layer. VI is a sharp departure from AI, which produces a *duplicate* of the human intellect in a material energy mechanism (e.g. a microprocessor or a network of such). AI is therefore non-vacuous. VI approaches non-vacuity (AI) to the degree of its performance in a [Turing test](http://dbpedia.org/page/Turing_test).

##<a name="regex"/>Regex Ranking

The dispatch algorithm rewards event patterns for *specificity*. The intuition is that the more specific the pattern, the more semantics are understood by it, and thus, the more appropriate the response of its event handler. So, the pattern `^youtube .*` will be ranked higher than `^.*$`. Since a node is designed to rely heavily on its local cache of mapped regexes, authors will need to work at making the regex's they publish very specific. Otherwise they will be outranked in a nodes' cache. A published list of ranking rules is provided to help authors. For example, the current algorithm removes `[]`-groups and the ranking = the length of the residual regex. A `^` at the start of the pattern gets a point boost, as does a `$` at the end of a pattern. Wildcards get points deducted, and so on. This approach requires linear time matching and therefore does not scale. A matcher [similar to setfiles in SELinux](https://www.csee.umbc.edu/wp-content/uploads/2014/03/Miner-Pairing-Strings-RegExps.pdf) is currently being investigated. The algorithm uses a graph-based matching approach to achieve matches in constant polynomial time.

##<a name="sensors"/>Sensors

A sensor monitors a 'physical channel' as a source for events. The current implementation supports HTTPSensor (web traffic), OverlaySensor (P2P traffic), and GraphSensor (local Index state changes).

##<a name="listeners"/>Listeners

A listener is a set of RDF statements that map an event to a handler. The primary language for describing listeners is the Vacuous Intermediary Automata (via) Ontology, a.k.a the Virtual Intelligence Authoring Ontology. The file extension for listeners and related data is .rq. Each .rq file is loaded in a graph context that matches file://<path>, where <path> is the absolute path of the file. This context IRI is linked to `via:ListenerContext` via `rdfs:subClassOf` at load time. The scope of certain via declarations, such as `via:override` and `via:prompt`, are restricted to the `via:ListenerContext` at event match time. 

Events are of two types: free-form and semantic. Free-form events are described using `via:pattern`. Semantic events are described using `via:sparql`. Each event captures data in capture groups. Capture groups are ordered by number and can be labeled using the `via:labels` property. Free-form events use Regex capture groups, or typed patterns. Queries use SPARQL projection items to capture groups. Unlike free form events, query events trigger a series of "result" events, each of which is dispatched to the `via:notify`. Below are some examples of listener descriptions:

```xml

	<!-- 
		Allows an EventHandler to listen to the input channel.
	  -->
    <via:Listener rdf:about="#youtube-url-adaptor">
 		<rdfs:comment>Pulls video URL from youtube.com</rdfs:comment>
 		<rdfs:label>Youtube Request Listener</rdfs:label>
 		<via:event rdf:resource="#youtube_cmd" />
		<via:notify rdf:resource="#youtube-fetch-videos" />
 	</via:Listener>

 	<via:EventHandler rdf:about="#youtube-fetch-videos">
 		<via:config>youtube.data</via:config>
 		<via:json rdf:resource="https://www.googleapis.com/youtube/v3/search?part=snippet&amp;type=video&amp;q=${1}&amp;key=${key}" />
		<via:translator rdf:resource="http://localhost:8080/rudi-adaptors/a/youtube/youtube-api-results.xsl" />
 		<via:log>Checking youtube.com for videos about '${1}'</via:log>
 	</via:EventHandler>
 	
 	<via:Input rdf:about="#youtube_cmd">
 		<via:pattern rdf:datatype="&via;Regex">^youtube\s*(.*)$</via:pattern>
 		<via:labels>Youtube Search Terms</via:labels>
 	</via:Input>

 	<via:Listener rdf:about="#listening-adaptor">
 		<rdfs:comment>SPARQL Listener example</rdfs:comment>
 		<rdfs:label>SPARQL Listener example</rdfs:label>
 		<via:event rdf:resource="#get_videos" />
		<via:notify rdf:resource="#moz-get-backlink-stats" />
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
 		<via:labels>Listen for new Youtube vids submitted to the Index</via:labels>
</via:TupleQuery>

<!-- 
   Allows a SPARQL query to listen to the response channel. 
  -->
 	<via:Listener rdf:about="#test-query-listener">
 		<rdfs:comment>SPARQL Listener example</rdfs:comment>
 		<rdfs:label>SPARQL Listener example</rdfs:label>
 		<via:event rdf:resource="#get_videos" />
		<via:notify rdf:resource="#virtuoso-sponger" />
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
 		<via:label>Listen for new Youtube vids submitted to the Index</via:label>
	</via:TupleQuery> 	

 	<via:Listener rdf:about="#test-query-listener-2">
 		<rdfs:comment>SPARQL Listener example (send email)</rdfs:comment>
 		<rdfs:label>SPARQL Listener example (send email)</rdfs:label>
 		<via:event rdf:resource="#get_videos-hold" />
		<via:notify rdf:resource="#john-doe" />
 	</via:Listener>
```

##<a name="placeholders"/>Placeholders

EventHandlers describe responses that are triggered by an event. The handler's hyperdata is a template that may contain placeholders. These placeholders are swapped for their values just before the event is dispatched to the `via:notify`. The `${number}` placeholder, where "number" is an integer value, is reserved and is swapped for the groups captured by the event. The placeholders `${id}`, `${key}`, `${url}`, and `${token}` are also reserved and are swapped for the config values associated with the `via:config` API identity string. The API configuration settings are located in the settings.ini file. The `${channel}` tag is replaced for the event or response IRI, depending on the context.

Custom placeholders may be used, and are swapped by custom TemplateProcessors, which are [Java Service Providers](https://docs.oracle.com/javase/tutorial/ext/basics/spi.html) that are called to process the EventHandler descriptions just prior to dispatch.

##<a name="channels"/>Channels

**Channels** are virtual communications conduits within RUDI. The two primary types of channels are input and response. The packets these channels carry are comprised of two parts:

1. An RDF URI whose subject is:
   a. A graph SPARQL query that registers on the (structured) input channel, or
   b. Plain-text that registers on the (free-form) input channel. Bots and web services listen for keywords on this channel.
2. Descriptions linking the packet to the Public Key of the originator, optional Public Key of the intended receipient, and other information. Descriptions using the Via ontology constitutes the packet header. Here are some of the packet header properties:
   1. Dispatch log - A cumulative list of endpoints to which the event has been dispatched, from the time of the first invocation up to the current node, and by whom. Each router checks their dispatch targets against this list of called endpoints to avoid duplicating replies, then appends its invocations to the log before dispatching the event.  Each peer also checks its peers list against the list of previous dispatchers before relaying the packet.
   2. Time to live (TTL) - In some cases a packet could circulate endlessly, for example if downstream nodes continually join the token ring. The TTL is a counter which each RUDI peer decrements. The peer which decrements the counter to 0 may dispatch the event but must not route the packet.
   3. Altcoin address - The altcoin type and address for events dispatched to sponsor nodes.

Channels are RESTfully navigated (the user requests some data to start with, then browses the results and requests the "rest" as needed). To enable this mode of browsing, service responses are converted to hyperdata (the resources are linked to resources contained elsewhere in the great global graph).

##<a name="sponsors"/>Sponsors

Events may be handled by a sponsor node which is configured by the RUDI peer. Once configured, EventHandlers which dispatch to the sponsor node may be registered with RUDI. A sponsor node collects listeners whose EventHandlers describe HTML5 content instead of endpoint invocations. Replies from sponsor nodes are transmitted back to the event originator along the Sponsor channel.


##<a name="listenerSpec"/>Listener Specification

| Tag                  | Description                              |         Required         | Cardinality |
| :------------------- | :--------------------------------------- | :----------------------: | :---------: |
| **via:Listener**     | A RUDI listener                          |                          |      n      |
| - rdfs:comment       | brief documentation                      |          false           |      n      |
| - rdf:label          | human-readable name                      |          false           |      n      |
| - via:event          | event to monitor; can be of type `via:Input` or `via:Query` |           true           |      n      |
| - via:notify         | A `via:EventHandler`, the URI of an entity with `via:cep` (communication endpoint) properties, such as a [LDN URI](https://www.w3.org/TR/2016/WD-ldn-20160726/), a SMS phone number, a RUDI broadcast topic, or an email inbox, to which the `via:event` is passed |           true           |      n      |
| **via:EventHandler** | accepts an event to disptach; the captured data is passed to via:call values via ${index} placeholders, where 'index' is the index of the captured datum |                          |             |
| - via:config         | the config in the settings.ini file which registers the custom `${tags}` to be used in this handler, and the endpoint attributes by which the via:call properties are sandboxed (the domain, port, and protocol assigned by the `via:config` must match thosed used in the `via:call` or a Security violation is thrown) |           true           |      1      |
| - via:json           | a `via:call` property whose value is the web service call to which the captured data is dispatched, and which produces JSON; the JSON is RDFized and stored in the Index |          false           |      n      |
| - via:rdf-xml        | a `via:call` property whose value is the web service call to which the captured data is dispatched, and which produces RDF-XML; the RDF is stored in the Index |          false           |      n      |
| - via:translator     | the URL of an XSLT sheet which translates the XMLized output of a via:json call into RDF+XML. The http://rudi.TAG.placeholders.vacuity.ai is a placeholder URL prefix which is substituted by RUDI for the URL path configured by the `rudi.TAG` property in settings.ini. The property can, for example, point to a resource path on the RUDI host. The path property must be absolute. Trust of the via:translator URL is provided by `via:signature`. | when via:json is present |      1      |
| - via:query          | the `via:Query` to which events are passed, the result set is RDFized and added to the Index |          false           |      n      |
| - via:log            | logs the dispatch                        |          false           |      n      |
| - via:import         | the `via:Query` whose projection elements may serve as placeholders for their values in this `via:EventHandler` |          false           |      n      |
| **via:signature**    | the [XML Signature](https://web-payments.org/vocabs/signature#XmlSignature) of the resource described by this property; the `rudi.translators.validate` and `rudi.call.validate` boolean properties in settings.ini control whether via:call and via:translator values are validated (***[TODO](http://docs.oracle.com/javase/7/docs/technotes/guides/security/xmldsig/XMLDigitalSignature.html)***). The Index will need to contain a nix:Trust idea linking the RUDI endpoint to the signer, and the idea needs to be signed by the RUDI endpoint. |          false           |      1      |
| **via:Input**        | captures events at RUDI's sensors, e.g. the web service controller |                          |             |
| - via:pattern        | captures data from an event pattern of type `rdf:datatype`. The `via:Input` is a `rdfs:subClass` of `rdf:List` and `via:pattern` is a `rdfs:subProperty` of `rdf:first`. The `rdf:first`- ness of `via:pattern` is merely syntatic sugar. Since some quad store frameworks may not support inferring sub properties of `rdf:first`, it is safer to use `rdf:first` explicily. The list items describe a hybrid pattern comprised of patterns of various `rdf:datatype`. The `via:pattern` specifes a *typed pattern* if its `rdf:datatype` property is a value other than `via:Regex`. For type `xsd:dateTime`, the `via:pattern` value specifies the date format. All other non-regex patterns have sample values which conform to the `rdf:datatype`. A list of supported typed patterns are provided in `rudi-adaptor.rdf`. |           true           |      n      |
| - via:labels         | a comma-delimited list, each item labels the datum at the corresponding index in the event pattern. An element of the list may be a URI given as a qname (nsRef:localPart), where the nsRef is known locally or contained within the RUDI Index. Escape comma using `\,` and semi-colon with `\:` |          false           |      n      |
| - via:context        | the `via:Query` to which events are passed, the result set is RDFized and added to the Index |                          |             |
| **via:Query**        | captures update events occuring in the Index; types are `rdf:TupleQuery`, `rdf:GraphQuery`, and `rdf:BooleanQuery` |                          |             |
| - via:sparql         | the sparql query whose projection captures data from the update event. The projection element names are numbers corresponding to indecies referenced in the Event Handler. The projection item name may be a number followed by the string \_label, for example ?1\_email. Each query result triggers the Event Handler. |           true           |      n      |
| - via:labels         | comma-delimited list of labels of data captured by numbered projection elements. |          false           |      n      |

##<a name="localGraph"/>Local Graph Structure

The local Graph is designed to be RESTfully navigated. Here is a diagram of the graph's basic structure, followed by a description of the entity types:

![RIGS](https://docs.google.com/drawings/d/1vOwOWbUxJytdbS8pVr21jkUnWY1vhGxi584WGTt9x-I/pub?w=1724&h=1052)

* *User* - the IRI of the user who originated an event at a RUDI sensor
* *Event Handler* - the IRI value of a `via:notify`
* `via:Channel` - an IRI representing an event detected by RUDI at one of its sensors
* `via:Response` - the IRI of the response generated by a `via:call` or `via:query`
* `via:Communication_response` - the native idea model of the `via:Response`
* `via:QueryResult` - a single result in the result set of a `via:query`
* `via:Hit` - a single result in the result set of a `via:event` of type `via:Query`
* `via:Projection` - the projection variable/value binding contained in a `via:QueryResult` or `via:Hit`
* `via:Alert` - the IRI of a stimulating Index update event scoped to a `via:notify` that ranges over a `via:Query`, and where the update event spans a document insert procedure, insertion of a list of statements, or insertion of a single statement. The `via:notify` should use the `${context}` to restrict the results of the query to the context IRI provided by the update event, since this context is always the IRI of the *Reply*.

## <a name="dialog"/>Dialog

The previous examples used `via:Input` to capture data from events and pass that data to `via:EventHandler` for dispatch. We can add to the match logic the constraint that certain statements be present in the Index at the time the event matches. If the event is accompanied by an RDF model, then the model is added to the Index under the graph context IRI matching the event (i.e. channel) IRI. The input model can, for example, provide contextual information. The input graph (and the Index in general) is probed by linking a `via:Input` to a SPARQL query using `via:isNotEmpty`. The object is a `via:TupleQuery` which is tested after the `via:event` matches. If the query result set is empty, the event is dropped, save where the `via:ListenerContext` links one or more nodes in the query to `via:prompt`, in which case RUDI will queue the event and emit the prompts in accordance with the `prompt.policy` setting. The event IRI is linked to `rdf:List` via `rdfs:subClassOf`. After an event has exited the dispatch decision gate, RUDI fetches the event list and fires the first element if one is present, then removes the event from the list. RUDI then adds its queued event to the end of the list. Here is an example:

```xml
	<via:Input rdf:about="#email">
 		<via:pattern rdf:datatype="&via;Regex">
          (?i)^MY EMAIL IS ([a-zA-Z0-9_\-\.]+)@([a-zA-Z0-9_\-\.]+)\.([a-zA-Z]{2,5})$
      	</via:pattern>
 		<via:labels>Email Address</via:labels>
 	</via:Input>

 	<via:Input rdf:about="#email2">
      	<via:isNotEmpty rdf:resource="#currentContext">
 		<via:pattern rdf:datatype="&via;Regex">
          ^([a-zA-Z0-9_\-\.]+)@([a-zA-Z0-9_\-\.]+)\.([a-zA-Z]{2,5})$
      	</via:pattern>
 		<via:labels>Email Address</via:labels>
 	</via:Input>
      
    <rdf:Resource rdf:about="#currentContext">
<![CDATA[
SELECT ?email
WHERE {
	graph ${channel}{
		?user foaf:mbox ?email .
		?user sioc:owner_of ${channel}
	}
}
]]>
    </rdf:Resource>
	<owl:Property rdf:about="&foaf;mbox">
		<via:prompt>What is you're email?</via:prompt>
	</owl:Property>
```

The VI author may need to detect the absence of certain statements. This is achieved by grouping required properties under a super property. For example:

```xml
	<owl:ObjectProperty rdf:about="userInfoProperty">
		<rdfs:label>A user data property.</rdfs:label>
	</owl:ObjectProperty>

	<owl:Property rdf:about="&foaf;mbox">
		<rdfs:subPropertyOf rdf:resource="userInfoProperty"/>
	</owl:ObjectProperty>

	<owl:Property rdf:about="&dc;educationLevel">
		<rdfs:subPropertyOf rdf:resource="userInfoProperty"/>
	</owl:ObjectProperty>

	<owl:Property rdf:about="&gr;PaymentMethodCreditCard">
		<rdfs:subPropertyOf rdf:resource="userInfoProperty"/>
	</owl:ObjectProperty>
```
Now the listener can now use `via:isEmpty` to test whether any information required to complete the interaction has not yet been provided. 

```xml
	<via:TupleQuery rdf:about="#userInfo">
		<rdfs:label>Fetch uncollected user properties</rdfs:label>
 		<via:sparql>
<![CDATA[
SELECT ?missingProperty
WHERE {
	graph ${channel} {
		?user ?missingProperty ?o .
		?user sioc:owner_of ${channel}
	    ?missingProperty rdfs:subPropertyOf myonto:userInfoProperty .
		FILTER (!bound(?o))
	}
}
]]>
		</via:sparql>
 		<via:labels>Missing Property</via:labels>
	</via:TupleQuery> 	

    <via:Listener rdf:about="#createTransaction">
 		<rdfs:comment>Start using the transaction service</rdfs:comment>
      	<via:isEmpty rdf:resource="#userInfo"/>
      	<via:override rdf:dataType="java://java.util.Properties">
          # values options are 'random' and 'all'
          prompt.policy=random
      	</via:override>
 		<rdfs:label>Transaction request listener</rdfs:label>
 		<via:event rdf:resource="#transaction_cmd" />
		<via:notify rdf:resource="#executeTransaction" />
 	</via:Listener>
```

The `via:override` property allows a node setting to be overridden in the `via:ListenerContext`, and in this example, the way RUDI handles prompts is declared for the current `via:ListenerContext` by overriding the `prompt.policy`. 

The event handler in this case is a query that inserts a set of triples under the graph context `${channel}`. Since the `${channel}` is linked to the original response via `sioc:has_reply`, the user can register listeners that detect `sioc:has_reply`. When responding, it includes a statement `${response_channel} sioc:has_reply []`, where `[]` is substituted for the RUDI-generated channel id. The resources `via:StringInput`, `via:Response`, `via:QueryResults`, and `via:Alert` are linked to `via:Channel` via `rdfs:subClassOf`. The input `via:Channel` history can thus be probed using SPARQL Path Property expression on `sioc:has_reply`, like so:

```xml
    <rdf:Resource rdf:about="#currentAndPreviousContexts">
<![CDATA[
SELECT ?email
WHERE {
	graph ?channel{
		?user foaf:mbox ?email .
		?user sioc:owner_of ?channel
		OPTIONAL {
			${channel} sioc:has_reply+ ?channel
			{
				{${channel} rdf:type via:InputString}
				UNION
				{${channel} rdf:type/rdfs:subClassOf* via:Query}
			}
		}
	}
}
]]>
    </rdf:Resource>
```

