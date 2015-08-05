package io.suggest.sjs.common.view.safe

import io.suggest.primo.{IUnderlying, TypeT}

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 14:23
 * Description: Заготовка для разработки совместимых safe-врапперов.
 */

object ISafe {

  /**
   * Генератор анонимной фунции-экстрактора underlying-элемента.
   * @tparam Tmin Минимальный возвращаемый тип underlying-элемента.
   */
  def extractorF[Tmin <: js.Object] = {
    { el: ISafe { type T <: Tmin} =>
      el._underlying
    }
  }

}

trait ISafe extends TypeT with IUnderlying {

  override type T <: js.Object

  /** wrapped-элемент, для которого реализуется безопасный доступ. */
  override def _underlying: T

}
