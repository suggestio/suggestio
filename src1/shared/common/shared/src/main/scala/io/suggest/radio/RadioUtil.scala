package io.suggest.radio

import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.16 14:51
  * Description: Пошаренная утиль для ble-маячков.
  */
object RadioUtil {

  def DBM_MAX = 20
  def DBM_MIN = -120

  def isRssiValid(rssi: Int): Boolean = {
    (rssi <= DBM_MAX) &&
    (rssi >= DBM_MIN)
  }

  /**
    * Оценка расстояния до маячка.
    *
    * @param distance0m Расстояние в метрах, на которой излучается сигнал с rssi0.
    * @param rssi0 Значение RSSI для сигнала на расстоянии distance0.
    * @param rssiOpt Текущее значение RSSI сигнала, до которого требуется оценить расстояние.
    *
    * @return None, если невозможно определить расстояние на основе указанных данных.
    *         Some() с оценкой расстояния до маячка в метрах.
    *
    * @see [[http://developer.radiusnetworks.com/2014/12/04/fundamentals-of-beacon-ranging.html]]
	  * @see [[http://stackoverflow.com/questions/21338031/radius-networks-ibeacon-ranging-fluctuation]]
    */
  def calculateAccuracy(distance0m: Int, rssi0: Int, rssiOpt: Option[Int]): Option[Double] = {
    for {
      rssi <- rssiOpt
      if isRssiValid( rssi )
    } yield {
      // The iBeacon distance formula uses txPower at 1 meters, but the Eddystone
      // protocol reports the value at 0 meters. 41dBm is the signal loss that
      // occurs over 1 meter, so we subtract that from the reported txPower.

      // 2016.nov.25: Всплыла проблема с eddystone-маячками от MS.spb.ru:
      // Они сообщают неправильный txpower: -61 dBM в качестве rssi0 (забыли перекалибровать, т.к. ibeacon 1 метр -61 dbm).
      val rssi0Fixed = if ((distance0m ==* 0) && (rssi0 < -60)) {
        -24
      } else {
        rssi0
      }

      // Тут вычисляем поправку для основной формулы на основе дистанции до излучателя:
      // Для ibeacon = 0 dBm, для eddystone -41 dBm.
      val txDiffDbm = Math.max(0, 1 - distance0m) * -41

      val ratio = rssi * 1.0 / (rssi0Fixed + txDiffDbm)

      val accuracy = if (ratio < 1.0) {
        Math.pow(ratio, 10)
      } else {
        0.89976 * Math.pow(ratio, 7.7095) + 0.111
      }

      accuracy
    }
  }

  def calculateAccuracy(signal: MRadioSignal): Option[Double] = {
    calculateAccuracy(
      distance0m  = signal.typ.distance0m.get,
      rssi0       = signal.rssi0
        .orElse( signal.typ.rssi0 )
        .get,
      rssiOpt     = signal.rssi,
    )
  }

}
