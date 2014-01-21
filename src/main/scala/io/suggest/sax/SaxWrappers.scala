package io.suggest.sax

import org.xml.sax._
import scala.annotation.tailrec

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


/** Вспомогательные враппер над экземпляром Attributes. */
trait AttributesWrapper extends Attributes {
  def wrappedAttributes: Attributes
  def getLength: Int = wrappedAttributes.getLength
  def getURI(index: Int): String = wrappedAttributes.getURI(index)
  def getLocalName(index: Int): String = wrappedAttributes.getLocalName(index)
  def getQName(index: Int): String = wrappedAttributes.getQName(index)
  def getType(index: Int): String = wrappedAttributes.getType(index)
  def getValue(index: Int): String = wrappedAttributes.getValue(index)
  def getIndex(uri: String, localName: String): Int = wrappedAttributes.getIndex(uri, localName)
  def getIndex(qName: String): Int = wrappedAttributes.getIndex(qName)
  def getType(uri: String, localName: String): String = wrappedAttributes.getType(uri, localName)
  def getType(qName: String): String = wrappedAttributes.getType(qName)
  def getValue(uri: String, localName: String): String = wrappedAttributes.getValue(uri, localName)
  def getValue(qName: String): String = wrappedAttributes.getValue(qName)
}


/** Для ignore-case проверок аттрибутов используется этот враппер. */
trait AttributesWithIgnoreCase extends Attributes {
  def getValueIgnoreCase(qName: String) = getValueIgnoreCaseI(qName, 0, getLength)

  @tailrec private def getValueIgnoreCaseI(qName: String, i: Int, len: Int): String = {
    if (i < len) {
      val qName = getQName(i)
      if (qName equalsIgnoreCase qName) {
        getValue(i)
      } else {
        getValueIgnoreCaseI(qName, i + 1, len)
      }
    } else null
  }
}


/** Класс, добавляющий разного функционала к штатным аттрибутам используя вышеописанные Attributes-трейты. */
case class RichAttributesWrapper(wrappedAttributes: Attributes) extends AttributesWrapper with AttributesWithIgnoreCase


/** Реализация Attributes, которая отражает пустые аттрибуты. */
trait EmptyAttributes extends Attributes {
  def getLength: Int = 0
  def getURI(index: Int): String = null
  def getLocalName(index: Int): String = null
  def getQName(index: Int): String = null
  def getType(index: Int): String = null
  def getValue(index: Int): String = null
  def getIndex(uri: String, localName: String): Int = -1
  def getIndex(qName: String): Int = -1
  def getType(uri: String, localName: String): String = null
  def getType(qName: String): String = null
  def getValue(uri: String, localName: String): String = null
  def getValue(qName: String): String = null
}


