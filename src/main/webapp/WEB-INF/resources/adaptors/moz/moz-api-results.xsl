<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" omit-xml-declaration="no" />

<xsl:template match="/">

 <rdf:RDF xml:base="http://vacuity.ai/schemas/via"
     xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
     xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
     xmlns:dc="http://purl.org/dc/elements/1.1/"
     xmlns:sioc="http://rdfs.org/sioc/ns#"
 >

  	<sioc:Site rdf:about="{/root/uu/text()}">
 		<dc:date rdf:parseType="unixEpoch"><xsl:value-of select="{ulc/text()}"/></dc:date>
 	</sioc:Site>
 
<xsl:for-each select="/root/row">

  	<sioc:Forum rdf:about="{uu/text()}">
<xsl:if test="ut/text()">
 		<dc:title><xsl:value-of select="{ut/text()}"/></dc:title>
</xsl:if>
 		<sioc:links_to rdf:resource="${1}"/>
 		<foaf:logo rdf:resource="http://richannel.org/favicon.ico"/>
 		<sioc:has_host rdf:resource="{/root/uu/text()}"/>
 	</sioc:Forum>
 	
</xsl:for-each>

</rdf:RDF> 
 
</xsl:template>

</xsl:stylesheet>