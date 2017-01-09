package io.suggest.sjs.common.vm.util

import io.suggest.sjs.common.vm.of.OfHtmlElement
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 15:33
 * Description: Интерфейс для доступа к id элемента в DOM.
 */
trait IDomIdApi {
  /** Соотносится ли указанный dom id с текущей vm?
    * Да, по дефолту, для упрощения override'а метода.
    */
  def isDomIdRelated(id: String): Boolean
}

trait IDomIdApiImpl extends IDomIdApi {
  def isDomIdRelated(id: String): Boolean = {
    true
  }
}

trait DomId extends IDomIdApiImpl {

  /** id элемента в рамках DOM. */
  def DOM_ID: String

  override def isDomIdRelated(id: String): Boolean = {
    super.isDomIdRelated(id) && id == DOM_ID
  }

}


/** Бывает что нужны динамические id на основе какого-то параметра какого-то типа.
  * Тут очень абстрактный трейт для более конкретный trait-реализаций этой функции. */
trait DynDomId extends IDomIdApi {

  type DomIdArg_t

  /** Сборка DOM_ID на основе какого-то аргумента произвольного типа. */
  def getDomId(arg: DomIdArg_t): String

}

/** Сырая эксплутация аргумента для генерации DOM_ID. */
trait DynDomIdToString extends DynDomId {
  def getDomId(arg: DomIdArg_t): String = {
    arg.toString
  }
}

/** Аргумент для генерации dom id -- это строка, которая всырую включается в итоговый dom_id. */
trait DynDomIdRawString extends DynDomId {
  override type DomIdArg_t = String
  def getDomId(arg: DomIdArg_t): String = {
    arg
  }
}


trait DomIdPrefixed extends DynDomId with IDomIdApiImpl {

  def DOM_ID_PREFIX: String

  abstract override def getDomId(arg: DomIdArg_t): String = {
    DOM_ID_PREFIX + super.getDomId(arg)
  }

  override def isDomIdRelated(id: String): Boolean = {
    super.isDomIdRelated(id) && id.startsWith(DOM_ID_PREFIX)
  }

}


trait DynDomIdIntOffT extends DynDomId {
  override type DomIdArg_t = Int
  protected def _DOM_ID_OFFSET: Int
  abstract override def getDomId(arg: DomIdArg_t): String = {
    super.getDomId(arg + _DOM_ID_OFFSET)
  }
}


/** Дополнительно суффиксовать сгенеренные dom id строкой-константой. */
trait DomIdSuffix extends DynDomId with IDomIdApiImpl {

  /** Константа-суффикс, приписывается к каждому id. */
  protected def DOM_ID_SUFFIX: String

  abstract override def getDomId(arg: DomIdArg_t): String = {
    super.getDomId(arg) + DOM_ID_SUFFIX
  }

  override def isDomIdRelated(id: String): Boolean = {
    super.isDomIdRelated(id) && id.endsWith(DOM_ID_SUFFIX)
  }

}


/** Поддержка проверки id элемента в of-фреймворке. */
trait OfHtmlElDomIdRelated extends OfHtmlElement with IDomIdApi {
  abstract override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
    super._isWantedHtmlEl(el) && isDomIdRelated(el.id)
  }
}
