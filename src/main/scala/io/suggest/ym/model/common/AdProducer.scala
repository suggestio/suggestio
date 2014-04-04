package io.suggest.ym.model.common

import io.suggest.ym.model.AdShowLevel

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 17:08
 * Description: Очень абстрактный рекламодатель.
 */

trait AdProducerStatic[T <: AdProducer[T]] extends AdNetMemberComboStatic[T]

trait AdProducer[T <: AdProducer[T]] extends AdNetMemberCombo[T] {

  def isAdProducer: Boolean = true

  /**
   * Сколько максимум рекламных карточек может публиковаться ДАННЫМ продьюсером на указанном уровне отображения.
   * Это не статическая фунцкия и оной она быть не может.
   * @param sl id уровня отображения.
   * @return Неотрицательное Int.
   */
  def getMaxOnShowLevel(sl: AdShowLevel): Int
}

