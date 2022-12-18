package kotlinw.util.stdlib

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import java.io.Reader
import javax.xml.namespace.QName
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.StartElement

fun Reader.createXmlEventReader(xmlInputFactory: XMLInputFactory = XMLInputFactory.newInstance()): XMLEventReader =
    xmlInputFactory.createXMLEventReader(this)

operator fun StartElement.get(attributeSimpleName: String): String? =
    getAttributeByName(QName(attributeSimpleName))?.value
