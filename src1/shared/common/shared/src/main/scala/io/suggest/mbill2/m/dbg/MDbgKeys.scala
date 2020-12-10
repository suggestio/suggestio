package io.suggest.mbill2.m.dbg

import enumeratum.values.{StringEnum, StringEnumEntry}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.05.17 10:47
  * Description: Модель ключей в MDebug.
  */

object MDbgKeys extends StringEnum[MDbgKey] {

  /** Инфа о рассчёте стоимости. */
  case object PriceDsl extends MDbgKey("p") {

    // Версии API
    /** Теперь выхлоп из pickle(), пожатый gzip'ом. */
    private def V_2 : DbgVsn_t  = 2.toShort

    /** Текущая (последняя) версия API. */
    def V_CURRENT   : DbgVsn_t  = V_2

  }


  /** Все возможные значения модели. */
  override def values = findValues

  def unapply(x: MDbgKey): Option[String] = {
    Some(x.value)
  }

}


/** Класс одного ряда. */
sealed abstract class MDbgKey(override val value: String) extends StringEnumEntry {

  override final def toString = value

}

