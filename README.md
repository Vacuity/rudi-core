    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public Licensealong with this program. If not, see http://www.gnu.org/licenses.


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
    
# Listeners

A listener maps an event to a handler. Events are of two types: free form and semantic. Free-form events are described using <via:pattern> and semantic events are described using <via:query>. Each event captures data in capture groups. Capture groups are ordered by number and can be labeled using the <via:label_*> property. Free form events use Regex capture groups, or typed patterned. Queries use SPARQL projection items to capture groups. Unlike free form events, query events trigger a series of "result" events, each of which is dispatched to the <via:notify>.

# Placeholders

EventHandlers describe responses that are triggered by an event. The handler's hyperdate is a template that may contain placeholders. These placeholders are swapped for their values just before the trigger is dispatched to the <via:call>. The ${number} placeholder, where "number" is an integer value, is reserved and is swapped for the groups captured by the event. The placeholders ${id}, ${key}, ${url}, and ${token} are also reserved and are swapped for the config values associated with the <via:config> API identity string. The API configuration settings are located in the api.config file.

Custom placeholders may be used, and are swapped by custom TemplateProcessors, which are Java plugins that are called to process the EventHandler descriptions just prior to dispatch. See the JavaDoc for details.

# Routing



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

