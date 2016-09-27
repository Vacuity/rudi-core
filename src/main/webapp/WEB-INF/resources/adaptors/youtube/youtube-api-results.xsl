<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" omit-xml-declaration="no" />

<xsl:template match="/">
 
 <rdf:RDF xml:base="http://www.vacuity.ai/onto/via"
     xmlns:via="http://www.vacuity.ai/onto/via/"
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
 	
<xsl:for-each select="/root/items">

<xsl:if test="id/videoId">
	
 	<sioc:Forum rdf:about="http://www.youtube.com/channel/{snippet/channelId/text()}">
 		<dc:title><xsl:value-of select="snippet/channelTitle"/></dc:title>
		<sioc:has_host rdf:resource="http://www.youtube.com"/>
 	</sioc:Forum>
 	
	<ma-ont:VideoTrack rdf:about="http://youtu.be/{id/videoId/text()}">
	    <dc:title><xsl:value-of select="snippet/title"/></dc:title>
	    <dc:description><xsl:value-of select="snippet/description"/></dc:description>
	<xsl:for-each select="/thumbnails">
	<xsl:for-each select="./*">
		<dbpedia:thumbnail rdf:resource="{url/text()}"/>
	</xsl:for-each>
	</xsl:for-each>
		<dc:date rdf:parseType="xsd:date"><xsl:value-of select="snippet/publishedAt"/></dc:date>
		<sioc:source rdf:resource="http://www.youtube.com"/>
		<sioc:has_container rdf:resource="http://www.youtube.com/channel/{snippet/channelId/text()}"/>
	</ma-ont:VideoTrack>

	<xsl:for-each select="snippet/thumbnails">
	<xsl:for-each select="./*">
	<dc:Image rdf:about="{url/text()}">
<xsl:if test="width">
		<ma-ont:frameWidth><xsl:value-of select="width"/></ma-ont:frameWidth>
</xsl:if>
<xsl:if test="height">
		<ma-ont:frameHeight><xsl:value-of select="height"/></ma-ont:frameHeight>
</xsl:if>
	</dc:Image>
	</xsl:for-each>
	</xsl:for-each>
	
<!--

unicode("foo")
	<rdf:Description>
		<foo:value>foo</foo:value>
	</rdf:Description>
langstring("foo", "fr")
	<rdf:Description>
		<foo:value xml:lang="fr">foo</foo:value>
	</rdf:Description>
date(2001-09-01)
	<rdf:Description>
		<foo:value rdf:parseType="xsd:Date">2001-09-01</foo:value>
	</rdf:Description>
number(12)
	<rdf:Description>
		<foo:value rdf:parseType="bar:Number">12</foo:value>
	</rdf:Description>
uri(http://www.w3.org/)
	<rdf:Description>
		<foo:value rdf:parseType="xsd:URI">http://www.w3.org/</foo:value>
	</rdf:Description>

http://www.vacuity.ai/onto/via/RegEx

-->
</xsl:if>	
</xsl:for-each> 	

  
</rdf:RDF>

</xsl:template>

</xsl:stylesheet>