package io.suggest.ble.eddystone

import io.suggest.ble.{IBeaconSignal, BeaconUtil}
import io.suggest.common.radio.IRadioSignal

import scalaz.{Validation, ValidationNel}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:36
  * Description: Модель payload'а по одному eddystone-фрейму.
  *
  * Для упрощения, поддержка URL пока отброшена.
  */
object EddyStoneUtil {

   /** Валидация id маячка. Ожидается строка в нижнем регистре. */
  def validateEddyStoneNodeId(eddyId: String): ValidationNel[String, String] = {
    Validation.liftNel( eddyId )(
      !_.matches( BeaconUtil.EddyStone.EDDY_STONE_NODE_ID_RE_LC ),
      "e.eddy.stone.id.invalid"
    )
  }

}


trait IEddyStoneSignal extends IRadioSignal {

  /** Тип фрейма EddyStone. В первом байте каждого eddystone-фрейма. */
  def frameType: MFrameType

}


/** Трейт для eddystone-фреймов, содержащих в себе данные txPower. */
trait IEddyStoneTxSignal extends IEddyStoneSignal with IBeaconSignal {

  /** Мощность антенны в дБ. */
  def txPower: Int

  override final def distance0m = 0
  override final def rssi0 = txPower

}
