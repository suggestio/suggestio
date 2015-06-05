package io.suggest.sc.sjs.m.msc.fsm

import io.suggest.sc.ScConstants.Search.Cats
import io.suggest.sc.sjs.v.vutil.VUtil
import org.scalajs.dom.Element

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.06.15 10:37
 * Description: Модель метаданных по категории.
 */

object MCatMeta {

  /**
   * Извлечь данные категории из html-тега категории.
   * @param catEl HTML-element, содержащий данные категории в оговоренном формате.
   * @return Опциональный результат.
   */
  def fromEl(catEl: Element): Option[MCatMeta] = {
    for {
      catId     <- VUtil.getAttribute(catEl, Cats.ATTR_CAT_ID)
      catClass  <- VUtil.getAttribute(catEl, Cats.ATTR_CAT_CLASS)
    } yield {
      MCatMeta(catId = catId, catClass = catClass)
    }
  }

}


/**
 * Экземпляр модели метаданных категории.
 * @param catId id категории.
 * @param catClass css-класс категории или подсказка оного.
 */
case class MCatMeta(catId: String, catClass: String)
