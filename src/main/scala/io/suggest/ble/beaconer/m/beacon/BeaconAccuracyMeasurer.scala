package io.suggest.ble.beaconer.m.beacon

import io.suggest.sjs.common.stat.TopBottomFiltered

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.16 16:13
  * Description: Статистическая замерялка примерного расстояния до маячка на основе данных сигнала.
  */
class BeaconAccuracyMeasurer
  extends TopBottomFiltered
{

  override type V = Double

  override def length: Int = 12

  override def sum(a: Double, b: Double): Double = {
    a + b
  }

  override def divide(a: Double, divider: Int): Double = {
    a / divider
  }

}
