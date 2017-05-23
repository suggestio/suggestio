package io.suggest.mbill2.m.dbg

import enumeratum._
import io.suggest.primo.IStrId

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.05.17 10:47
  * Description: Модель ключей в MDebug.
  */

/** Класс одного ряда. */
sealed abstract class MDbgKey
  extends EnumEntry
  with IStrId
{
  override final def toString = super.toString
}


/** Статическая enum-модель поля ключей MDebug. */
object MDbgKeys extends Enum[MDbgKey] {

  /** Инфа о рассчёте стоимости: сериализованный через boopickle блобик с инстансом PriceDsl. */
  case object PriceDsl extends MDbgKey {

    override def strId = "p"

    // Версии API
    def V_1         : DbgVsn_t  = 1.toShort
    def V_CURRENT   : DbgVsn_t  = V_1

  }


  /** Все возможные значения модели. */
  override def values = findValues

  def unapply(x: MDbgKey): Option[String] = {
    Some(x.strId)
  }

}
