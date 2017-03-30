package io.suggest.ble.eddystone

import io.suggest.ble.BeaconSignal
import io.suggest.common.radio.IRadioSignal

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:36
  * Description: Модель payload'а по одному eddystone-фрейму.
  *
  * Для упрощения, поддержка URL пока отброшена.
  *
  */
trait IEddyStoneSignal extends IRadioSignal {
  def frameType: MFrameType
}


/** Трейт для eddystone-фреймов, содержащих в себе данные txPower. */
trait IEddyStoneTxSignal extends IEddyStoneSignal with BeaconSignal {

  /** Мощность антенны в дБ. */
  def txPower: Int

  override final def distance0m = 0
  override final def rssi0 = txPower

}
