package io.suggest.sjs.common.vm.util

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

