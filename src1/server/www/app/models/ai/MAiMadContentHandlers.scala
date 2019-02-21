package models.ai

import enumeratum.values.{StringEnum, StringEnumEntry}
import japgolly.univeq.UnivEq
import org.xml.sax.helpers.DefaultHandler
import util.ai.AiContentHandler
import util.ai.sax.currency.cbrf.CbrfCurDayXmlSax
import util.ai.sax.weather.gidromet.GidrometRssSax


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.14 20:01
 * Description: Модель с доступными обработчиками контента.
 */

object MAiMadContentHandlers extends StringEnum[MAiMadContentHandler] {

  /** Sax/tika-парсер для rss-прогнозов росгидромета. */
  case object GidrometRss extends MAiMadContentHandler("gidromet.rss")

  /** SAX-парсер для выхлопов ЦБ РФ через XML API сайта. */
  case object CbrfXml extends MAiMadContentHandler("cbrf.xml")

  override def values = findValues

}


sealed abstract class MAiMadContentHandler(override val value: String) extends StringEnumEntry

object MAiMadContentHandler {

  implicit class MAiChOpsExt(val mch: MAiMadContentHandler) extends AnyVal {

    /** Собрать новый инстанс sax-парсера. */
    def newInstance(maim: MAiCtx): DefaultHandler with AiContentHandler = {
      mch match {
        case MAiMadContentHandlers.GidrometRss =>
          new GidrometRssSax(maim)
        case MAiMadContentHandlers.CbrfXml =>
          new CbrfCurDayXmlSax
      }
    }
  }

  @inline implicit def univEq: UnivEq[MAiMadContentHandler] = UnivEq.derive

}


/** Абстрактный результат работы Content-Handler'а. Это JavaBean-ы, поэтому должны иметь Serializable. */
trait ContentHandlerResult extends Serializable

