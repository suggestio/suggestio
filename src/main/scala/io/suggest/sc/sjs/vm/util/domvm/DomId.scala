package io.suggest.sc.sjs.vm.util.domvm

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 15:33
 * Description: Интерфейс для доступа к id элемента в DOM.
 */
trait DomId {

  /** id элемента в рамках DOM. */
  def DOM_ID: String

}
