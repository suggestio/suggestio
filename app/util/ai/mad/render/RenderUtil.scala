package util.ai.mad.render

import models.MAd
import models.ai.ContentHandlerResult

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.14 19:30
 * Description: Всякая утиль для рендереров-генераторов.
 */


/** Интерфейс рендереров карточек. Каждый рендерер берёт на вход результат прошлого рендерера (или исходную карточку)
  * и возвращает новый инстанс карточки с внемёнными модификациями, либо исходный инстанс, если не было изменений. */
trait MadAiRenderedT {

  /**
   * Компиляция одной карточки.
   * @param tplAd Исходная карточка.
   * @param args Аргументы рендера.
   * @return Фьючерс с новой карточкой.
   */
  def renderTplAd(tplAd: MAd, args: Map[String, ContentHandlerResult]): Future[MAd]
}