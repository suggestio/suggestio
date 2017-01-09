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

  // Изначально было Double в метрах. Но для простоты всё сменилось на Int в сантиметрах.
  override type V = Int

  override def length: Int = 12

  override def sum(a: V, b: V): V = {
    a + b
  }

  override def divide(a: V, divider: Int): V = {
    a / divider
  }

}
