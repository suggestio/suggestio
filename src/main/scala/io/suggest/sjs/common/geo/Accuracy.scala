package io.suggest.sjs.common.geo

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.08.15 17:05
 * Description: Трейты для сборки моделей js-геолокации.
 */

trait IHighAccuracy {
  def highAccuracy: Boolean
}

/** Реализация [[IHighAccuracy]] с точностью уровня bss. */
trait BssAccuracy extends IHighAccuracy {
  override def highAccuracy = false
}

