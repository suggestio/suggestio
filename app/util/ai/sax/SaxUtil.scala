package util.ai.sax

import io.suggest.sax.SaxContentHandlerWrapper
import org.xml.sax._
import org.xml.sax.helpers.DefaultHandler

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.11.14 16:25
 * Description: Утиль для SAX-парсеров проекта.
 */

/** Реализация обходчика дерева тегов с помощью стековый конечный автомат. */
trait StackFsmSax extends SaxContentHandlerWrapper with ErrorHandler {

  override def contentHandler: ContentHandler = handlersStack.head

  protected var handlersStack: List[TagHandler] = Nil
  implicit var locator: Locator = null

  /** Макс.глубина погружения в дерево тегов. Это позволяет избежать утечек RAM прервав обработку. */
  val maxTagDepth: Int = 100

  /** Текущая глубина погружения в теги. Обновляется через become() и unbecome(). */
  protected var tagDepth: Int = handlersStack.size


  override def setDocumentLocator(l: Locator): Unit = {
    // нельзя тут вызывать super! Т.к. это порождает обращение к handlersStack, который пустой.
    locator = l
  }

  /** Стековое переключение состояние этого ContentHandler'а. */
  def become(h: TagHandler) {
    tagDepth += 1
    if (tagDepth > maxTagDepth)
      throw new IllegalStateException("Tag depth limit exceeded.")
    handlersStack ::= h
  }

  /** Извлечение текущего состояния из стека состояний. */
  def unbecome(): TagHandler = {
    val result = handlersStack.head
    handlersStack = handlersStack.tail
    tagDepth -= 1
    result
  }


  /** Экщепшен на тему получения очень неожиданного тега. */
  def unexpectedTag(tagName: String) = {
    val ex = new SAXParseException(s"Unexpected tag: '$tagName' on state ${handlersStack.headOption.getClass.getSimpleName}.", locator)
    fatalError(ex)
    throw ex
  }


  /** Базовый интерфейс для написания обработчиков тегов в рамках этого парсера. */
  trait TagHandler extends DefaultHandler {
    def thisTagName: String
    def thisTagAttrs: Attributes

    def startTag(tagName:String, attributes: Attributes) {}

    def getTagNameFor(uri:String, localName:String, qName:String): String = {
      if (localName.isEmpty) {
        if (uri.isEmpty) {
          qName
        } else {
          val ex = new SAXParseException("Internal parser error. Cannot understand tag name: " + qName, locator)
          fatalError(ex)
          throw ex    // для надежности
        }
      } else {
        localName
      }
    }

    override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
      // Т.к. нет нормального неймспейса, усредняем тут localName и qName.
      super.startElement(uri, localName, qName, attributes)
      val tagName = getTagNameFor(uri, localName, qName)
      startTag(tagName, attributes)
    }

    def endTag(tagName: String) {
      if ((tagName equalsIgnoreCase thisTagName) && (handlersStack.head == this)) {
        onTagEnd(tagName)
        unbecome()
      } else {
        // Надо остановиться и выругаться на неправильно закрытый тег. Наверху оно будет обработано через try-catch
        unexpectedClosingTag(tagName)
      }
    }

    def unexpectedClosingTag(tagName: String) {
      fatalError(new SAXParseException(s"Unexpected closing tag: '$tagName', but close-tag '$tagName' expected.", locator))
    }

    /** Этот метод вызывается, когда наступает пора завершаться.
      * В этот момент обычно отпавляются в коллектор накопленные данные. */
    def onTagEnd(tagName: String) {}

    /** Выход из текущего элемента у всех одинаковый. */
    override def endElement(uri: String, localName: String, qName: String) {
      super.endElement(uri, localName, qName)
      val tagName = getTagNameFor(uri, localName, qName)
      endTag(tagName)
    }
  }


  /** Заготовка для самого начального обработчика докумена. С него начинается парсинг. */
  trait TopLevelHandlerT extends TagHandler {
    protected def illegalOp = throw new UnsupportedOperationException("Cannot get tag attributes for empty tags tree.")
    override def thisTagName: String = illegalOp
    override def thisTagAttrs: Attributes = illegalOp
    override def endTag(tagName: String): Unit = {}
  }


  /** Обработчик ненужных тегов. */
  class DummyHandler(val thisTagName: String, val thisTagAttrs: Attributes) extends TagHandler {
    /** Внутри начинается тег. */
    override def startTag(tagName: String, attributes: Attributes): Unit = {
      become(new DummyHandler(tagName, attributes))
    }
  }

}
