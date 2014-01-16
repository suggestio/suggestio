package io.suggest.sax

import org.xml.sax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.01.14 15:07
 * Description: Различные врапперы над интерфейсами SAX.
 */

trait SaxContentHandlerWrapper extends ContentHandler {
  def contentHandler: ContentHandler

  override def setDocumentLocator(locator: Locator) {
    contentHandler.setDocumentLocator(locator)
  }

  override def startDocument() {
    contentHandler.startDocument()
  }

  override def endDocument() {
    contentHandler.endDocument()
  }

  override def startPrefixMapping(prefix: String, uri: String) = {
    contentHandler.startPrefixMapping(prefix, uri)
  }

  override def endPrefixMapping(prefix: String) = {
    contentHandler.endPrefixMapping(prefix)
  }

  override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
    contentHandler.startElement(uri, localName, qName, attributes)
  }

  override def endElement(uri: String, localName: String, qName: String) {
    contentHandler.endElement(uri, localName, qName)
  }

  override def characters(ch: Array[Char], start: Int, length: Int) {
    contentHandler.characters(ch, start, length)
  }

  override def ignorableWhitespace(ch: Array[Char], start: Int, length: Int) {
    contentHandler.ignorableWhitespace(ch, start, length)
  }

  override def processingInstruction(target: String, data: String) {
    contentHandler.processingInstruction(target, data)
  }

  override def skippedEntity(name: String) {
    contentHandler.skippedEntity(name)
  }
}

