package io.suggest.sjs.common.view.safe

import io.suggest.primo.TypeT

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 14:23
 * Description: Заготовка для разработки совместимых safe-врапперов.
 */
trait ISafe extends TypeT {

  override type T <: js.Object

  /** wrapped-элемент, для которого реализуется безопасный доступ. */
  def _underlying: T

}
