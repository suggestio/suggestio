package util.ai.sax.currency.cbrf

import models.ai.AiParsers.AiParser
import models.ai._
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import util.PlayLazyMacroLogsImpl
import util.ai.AiContentHandler
import util.ai.sax.StackFsmSax

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.14 19:49
 * Description: Парсер для страниц сайта ЦБ РФ, содержащих XML-таблицу курсов за конкретный день.
 * @see [[http://www.cbr.ru/scripts/XML_daily.asp]]
 * @see [[http://www.cbr.ru/scripts/XML_daily_eng.asp]]
 */
class CbrfCurDayXmlSax
  extends DefaultHandler
  with StackFsmSax
  with AiContentHandler
  with PlayLazyMacroLogsImpl
{

  var _accRev: List[AiCurrency] = Nil


  override def startDocument(): Unit = {
    become(new TopLevelHandler)
  }

  class TopLevelHandler extends TopLevelHandlerT {
    override def startTag(tagName: String, attributes: Attributes): Unit = {
      super.startTag(tagName, attributes)
      if (tagName equalsIgnoreCase "ValCurs") {
        become(new ValCursTagHandler(attributes))
      } else {
        unexpectedTag(tagName)
      }
    }
  }

  class ValCursTagHandler(val thisTagAttrs: Attributes) extends TagHandler {
    override def thisTagName = "ValCurs"

    override def startTag(tagName: String, attributes: Attributes): Unit = {
      super.startTag(tagName, attributes)
      if (tagName equalsIgnoreCase "Valute") {
        become(new ValuteTagHandler(attributes))
      } else {
        unexpectedTag(tagName)
      }
    }
  }

  /** Обработка одной валюты. */
  class ValuteTagHandler(val thisTagAttrs: Attributes) extends TagHandler {
    override def thisTagName = "Valute"
    val acc = AiCurrencyAcc()

    override def startTag(tagName: String, attributes: Attributes): Unit = {
      super.startTag(tagName, attributes)
      val nextState = if (tagName equalsIgnoreCase "NumCode")
        new NumCodeTagHandler(tagName, attributes)
      else if (tagName equalsIgnoreCase "CharCode")
        new CharCodeTagHandler(tagName, attributes)
      else if (tagName equalsIgnoreCase "Nominal")
        new NominalTagHandler(tagName, attributes)
      else if (tagName equalsIgnoreCase "Name")
        new NameTagHandler(tagName, attributes)
      else if (tagName equalsIgnoreCase "Value")
        new ValueTagHandler(tagName, attributes)
      else
        new DummyHandler(tagName, attributes)
      become(nextState)
    }

    override def endTag(tagName: String): Unit = {
      // Закинуть накопленные данные в общий аккамулятор.
      _accRev ::= acc.toImmutable()
      super.endTag(tagName)
    }


    /** Обработка цифрового кода валюты ЦБ. Он обычно начинается с нулей, поэтому строковой. */
    class NumCodeTagHandler(val thisTagName: String, val thisTagAttrs: Attributes) extends SbTagHandler(3) {
      override def endTag(tagName: String): Unit = {
        acc.numCodeOpt = if (sb.length > 2) Some(sb.toString()) else None
        super.endTag(tagName)
      }
    }

    /** Обработка строкового международного кода валюты: USR, EUR и т.д. */
    class CharCodeTagHandler(val thisTagName: String, val thisTagAttrs: Attributes) extends SbTagHandler(3) {
      override def endTag(tagName: String): Unit = {
        acc.charCode = sb.toString().toUpperCase
        super.endTag(tagName)
      }
    }

    /** Номинал описываемой валюты. */
    class NominalTagHandler(val thisTagName: String, val thisTagAttrs: Attributes) extends SbTagHandler(5) {
      override def endTag(tagName: String): Unit = {
        acc.count = sb.toInt
        super.endTag(tagName)
      }
    }

    /** Класс для сбора названия имени валюты. Например: "Евро", "Фунт стерлингов Соединенного королевства" и т.д. */
    class NameTagHandler(val thisTagName: String, val thisTagAttrs: Attributes) extends SbTagHandler(128) {
      override def endTag(tagName: String): Unit = {
        acc.nameOpt = if (sb.nonEmpty) Some(sb.toString()) else None
        super.endTag(tagName)
      }
    }

    /** Значение курса для указанной валюты. */
    class ValueTagHandler(val thisTagName: String, val thisTagAttrs: Attributes) extends SbTagHandler(128) {
      override def endTag(tagName: String): Unit = {
        acc.course = sb.toString()
          .replace(',', '.')
          .toFloat
        super.endTag(tagName)
      }
    }

  }

  /** Многие теги просто накапливают инфу. Тут абстрактный класс, облегчающий это дело.
    * trait делать нельзя, т.к. sbInitSz должен уже быть определён в конструкторе. */
  abstract class SbTagHandler(sbInitSz: Int) extends TagHandler {
    val sb = new StringBuilder(sbInitSz)

    override def characters(ch: Array[Char], start: Int, length: Int): Unit = {
      super.characters(ch, start, length)
      sb.appendAll(ch, start, length)
    }
  }


  override def sourceParser: AiParser = AiParsers.SaxTolerant

  /**
   * Ключ-идентификатор, используемый для формирования карты результатов.
   * @return Строка, маленькими буквами и без пробелов.
   */
  override def stiResKey: String = "currency"

  /**
   * Вернуть накопленный результат парсинга.
   * Если результата нет или он заведомо неверный/бесполезный, то должен быть экзепшен с причиной.
   * @return Реализация модели ContentHandlerResult.
   */
  override def getParseResult: CurrenciesInfoBeanT = {
    val currMap = _accRev.iterator
      .map { cur => cur.charCode -> cur }
      .toMap
    new CurrenciesInfoBeanT {
      override def getMap = currMap
    }
  }
}


