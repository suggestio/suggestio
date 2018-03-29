package io.suggest.ble.eddystone

import enumeratum.values.{ShortEnum, ShortEnumEntry}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.17 12:05
  * Description: Модель типов фреймов EddyStone.
  */

object MFrameTypes extends ShortEnum[MFrameType] {

  /** UID-фрейм.
    * @see [[https://github.com/google/eddystone/tree/master/eddystone-uid]]
    */
  case object UID extends MFrameType(0x00) {

    /** Длина обычного UID-фрейма равна 18-байтам, но допустимы и 20-байтовые ответы,
      * где лишние байты в хвосте можно проигнорить. */
    override def frameMinByteLen = 18

  }


  override def values = findValues

}


/** Класс одного типа фрейма EddyStone. */
sealed abstract class MFrameType(override val value: Short) extends ShortEnumEntry {

  /** Байтовый код фрейма EddyStone. */
  final def frameCode: Short = value

  /** Минимальная длина фрейма в байтах. */
  def frameMinByteLen: Int

}
