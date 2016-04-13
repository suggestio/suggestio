package io.suggest.sjs.common.vm.util

import io.suggest.sjs.common.vm.of.OfHtmlElement
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 15:33
 * Description: Интерфейс для доступа к id элемента в DOM.
 */
trait DomId {

  /** id элемента в рамках DOM.
    * В случае [[IndexedDomId]] тут префикс элемента. */
  def DOM_ID: String

  /** Соотносится ли указанный DOM_ID с этой моделью. */
  def isDomIdRelated(id: String): Boolean = {
    // TODO Вынести эту реализацию куда-нибудь в отдельный трейт.
    id == DOM_ID
  }

}


/** Бывает что нужны динамические id на основе какого-то параметра какого-то типа.
  * Тут очень абстрактный трейт для более конкретный trait-реализаций этой функции. */
trait DynDomId {

  type DomIdArg_t

  def getDomId(arg: DomIdArg_t): String

}


/** Трейт для генерации id элементов по как "строка" + индекс.
  * id1, id2, id3... */
trait IndexedDomId extends DynDomId with DomId {

  override type DomIdArg_t = Int

  override def getDomId(arg: DomIdArg_t): String = {
    DOM_ID + arg
  }

  override def isDomIdRelated(id: String): Boolean = {
    id.startsWith(DOM_ID)
  }

}


/** Расширенная версия [[IndexedDomId]], дополнительно суффиксующая сгенеренные dom id строкой-константой. */
trait IndexedSuffixedDomId extends IndexedDomId {
  /** Константа-суффикс, приписывается к каждому id. */
  protected def DOM_ID_SUFFIX: String

  override def getDomId(arg: Int): String = {
    super.getDomId(arg) + DOM_ID_SUFFIX
  }

  override def isDomIdRelated(id: String): Boolean = {
    super.isDomIdRelated(id) && id.endsWith(DOM_ID_SUFFIX)
  }

}


/** DOM_ID, запрефиксованный строкой. */
trait PrefixedDomId extends DynDomId with DomId {

  override type DomIdArg_t = String
  override def getDomId(arg: DomIdArg_t): String = {
    arg + DOM_ID
  }

  override def isDomIdRelated(id: String): Boolean = {
    id.endsWith(DOM_ID)
  }

}


/** Поддержка проверки id элемента в of-фреймворке. */
trait OfHtmlElDomIdRelated extends OfHtmlElement with DomId {
  abstract override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
    super._isWantedHtmlEl(el) && isDomIdRelated(el.id)
  }
}
