<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" omit-xml-declaration="no" />

<xsl:template match="/">

 <rdf:RDF xml:base="http://vacuity.ai/schemas/via"
     xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
     xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
     xmlns:dc="http://purl.org/dc/elements/1.1/"
     xmlns:sioc="http://rdfs.org/sioc/ns#"
     xmlns:foaf="http://xmlns.com/foaf/0.1/"
 >
<xsl:for-each select="/root/items">
  	<sioc:Site rdf:about="{uu/text()}">
 		<dc:date rdf:parseType="unixEpoch"><xsl:value-of select="ulc"/></dc:date>
 		<dc:title><xsl:value-of select="ut"/></dc:title>
		<sioc:source rdf:resource="http://www.moz.com"/>
 	</sioc:Site> 	
</xsl:for-each>

</rdf:RDF> 
 
</xsl:template>

</xsl:stylesheet>