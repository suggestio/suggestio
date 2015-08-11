package io.suggest.sc.sjs.m.msc.fsm

import io.suggest.sc.ScConstants.Search.Cats
import io.suggest.sjs.common.view.safe.attr.SafeAttrElT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.06.15 10:37
 * Description: Модель метаданных по категории.
 */
@deprecated("FSM-MVM: no more categories, only hashtags.", "2015.aug.11")
object MCatMeta {

  /**
   * Извлечь данные категории из html-тега категории.
   * @param catEl HTML-element, содержащий данные категории в оговоренном формате.
   * @return Опциональный результат.
   */
  def fromEl(catEl: SafeAttrElT): Option[MCatMeta] = {
    for {
      catId     <- catEl.getAttribute(Cats.ATTR_CAT_ID)
      catClass  <- catEl.getAttribute(Cats.ATTR_CAT_CLASS)
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
@deprecated("FSM-MVM: no more categories, only hashtags.", "2015.aug.11")
case class MCatMeta(catId: String, catClass: String)
