<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" omit-xml-declaration="no" />

<xsl:template match="/">
 
 <rdf:RDF xml:base="http://www.vacuity.ai/onto/via/1.0"
     xmlns:via="http://www.vacuity.ai/onto/via/1.0/"
     xmlns:ma-ont="http://www.w3.org/ns/ma-ont/"
     xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
     xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
     xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
     xmlns:owl="http://www.w3.org/2002/07/owl#"
     xmlns:foaf="http://xmlns.com/foaf/0.1/"
     xmlns:dc="http://purl.org/dc/elements/1.1/"
     xmlns:sioc="http://rdfs.org/sioc/ns#"
     xmlns:dbpedia="http://dbpedia.org/property/"
 >
 	
<xsl:for-each select="/root/posts">
<xsl:if test="network = 'twitter'">
	
 	<sioc:Forum rdf:about="http://www.twitter.com">
 		<dc:title><xsl:value-of select="network"/></dc:title>
 	</sioc:Forum>
 	<sioc:Post rdf:about="{url/text()}">
 		<dc:title><xsl:value-of select="text"/></dc:title>
 		<dc:date rdf:datatype="http://www.w3.org/2001/XMLSchema#dateTime"><xsl:value-of select="posted"/></dc:date>
 		<foaf:depiction rdf:resource="{image/text()}"/>
 		<sioc:has_container rdf:resource="http://www.twitter.com"/>
<xsl:if test="network/user_mentions">
 		<sioc:links_to rdf:resource="{network/user_mentions/url/text()}"/>
</xsl:if>	<!-- if User -->
<xsl:if test="network/user_mentions">
	</xsl:if>	<!-- if User -->
 	</sioc:Post>
<xsl:if test="user">
 	<foaf:Person rdf:about="{user/url/text()}">
 		<dc:title><xsl:value-of select="text"/></dc:title>
 		<foaf:homepage rdf:resource="{user/url/text()}"/>
 		<foaf:name><xsl:value-of select="user/name"/></foaf:name>
 		<foaf:nick><xsl:value-of select="user/userId"/></foaf:nick>
 		<foaf:img rdf:resource="{user/image/text()}"/>
 	</foaf:Person>
</xsl:if>	<!-- if User -->

<xsl:if test="video_info">
	<ma-ont:VideoTrack rdf:about="{video_info/variants/url[1]/text()}">
	</ma-ont:VideoTrack>
</xsl:if>	<!-- if User -->


</xsl:if>	<!-- if twitter -->
<xsl:if test="network = 'googleplus'">
	
 	<sioc:Forum rdf:about="http://www.youtube.com/ss/">
 		<dc:title><xsl:value-of select="network"/></dc:title>
 	</sioc:Forum>

</xsl:if>	
<xsl:if test="network = 'instagram'">
	
 	<sioc:Forum rdf:about="http://www.youtube.com/ss/">
 		<dc:title><xsl:value-of select="network"/></dc:title>
 	</sioc:Forum>

</xsl:if>	
<xsl:if test="network = 'tumblr'">
	
 	<sioc:Forum rdf:about="http://www.youtube.com/ss/">
 		<dc:title><xsl:value-of select="network"/></dc:title>
 	</sioc:Forum>

</xsl:if>	
<xsl:if test="network = 'reddit'">
	
 	<sioc:Forum rdf:about="http://www.youtube.com/ss/">
 		<dc:title><xsl:value-of select="network"/></dc:title>
 	</sioc:Forum>

</xsl:if>	
<xsl:if test="network = 'flickr'">
	
 	<sioc:Forum rdf:about="http://www.youtube.com/ss/">
 		<dc:title><xsl:value-of select="network"/></dc:title>
 	</sioc:Forum>

</xsl:if>	
<xsl:if test="network = 'vimeo'">
	
 	<sioc:Forum rdf:about="http://www.youtube.com/ss/">
 		<dc:title><xsl:value-of select="network"/></dc:title>
 	</sioc:Forum>

</xsl:if>	
<xsl:if test="network = 'vine'">
	
 	<sioc:Forum rdf:about="http://www.youtube.com/ss/">
 		<dc:title><xsl:value-of select="network"/></dc:title>
 	</sioc:Forum>

</xsl:if>	
</xsl:for-each> 	

  
</rdf:RDF>

</xsl:template>

</xsl:stylesheet>