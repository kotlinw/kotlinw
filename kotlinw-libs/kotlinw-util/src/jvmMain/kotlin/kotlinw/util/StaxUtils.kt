package kotlinw.util

import java.io.Reader
import javax.xml.namespace.QName
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent

fun Reader.createXmlEventReader(xmlInputFactory: XMLInputFactory = XMLInputFactory.newInstance()): XMLEventReader =
    xmlInputFactory.createXMLEventReader(this)

operator fun StartElement.get(attributeNameLocalPart: String): String =
    getAttributeByName(QName(attributeNameLocalPart))?.value
        ?: throw IllegalStateException("Required attribute '$attributeNameLocalPart' does not exist.")
