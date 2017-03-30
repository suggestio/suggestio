package io.suggest.ble.eddystone

import enumeratum._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.17 12:05
  * Description: Модель типов фреймов EddyStone.
  */

/** Класс одного типа фрейма EddyStone. */
sealed abstract class MFrameType extends EnumEntry {

  /** Байтовый код фрейма EddyStone. */
  def frameCode: Short

  /** Минимальная длина фрейма в байтах. */
  def frameMinByteLen: Int

}


/** Модель типов фреймов EddyStone. */
object MFrameTypes {

  /** UID-фрейм.
    * @see [[https://github.com/google/eddystone/tree/master/eddystone-uid]]
    */
  case object Uid extends MFrameType {

    override def frameCode: Short = 0x00

    /** Длина обычного UID-фрейма равна 18-байтам, но допустимы и 20-байтовые ответы,
      * где лишние байты в хвосте можно проигнорить. */
    override def frameMinByteLen = 18

  }

}
