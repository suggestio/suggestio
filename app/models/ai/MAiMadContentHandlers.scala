package models.ai

import io.suggest.model._
import org.xml.sax.helpers.DefaultHandler
import util.ai.AiContentHandler
import util.ai.sax.weather.gidromet.GidrometRssSax

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.14 20:01
 * Description: Модель с доступными обработчиками контента.
 */

object MAiMadContentHandlers extends EnumMaybeWithName {
  protected sealed abstract class Val(val name: String) extends super.Val(name) {
    /** Собрать новый инстанс sax-парсера. */
    def newInstance(maim: MAiCtx): DefaultHandler with AiContentHandler
  }

  type MAiMadContentHandler = Val
  override type T = MAiMadContentHandler


  // Тут всякие доступные content-handler'ы.
  /** Sax-парсер для rss-прогнозов росгидромета. */
  val GidrometRss: MAiMadContentHandler = new Val("gidromet.rss") {
    override def newInstance(maim: MAiCtx) = new GidrometRssSax(maim)
  }

}


/** Абстрактный результат работы Content-Handler'а. Это JavaBean-ы, поэтому должны иметь Serializable. */
trait ContentHandlerResult extends Serializable

