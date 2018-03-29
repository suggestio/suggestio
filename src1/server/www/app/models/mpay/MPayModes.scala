package models.mpay

import enumeratum.values.{StringEnum, StringEnumEntry}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.17 11:24
  * Description: Режимы конфигураций платежных систем.
  * У яндекс-кассы есть "боевой" и "демо/тестовый" режимы.
  * У других -- наверное тоже.
  */

object MPayModes extends StringEnum[MPayMode] {

  /** Нормальный режим: продакшен, реальная работа с реальными деньгами. */
  case object Production extends MPayMode("prod") {
    override def isProd   = true
  }

  /** Тестовый (доменстрационный) режим работы. Виртуальные деньги. */
  case object Testing extends MPayMode("test") {
    override def isProd   = false
  }


  override def values = findValues

}


/** Класс модели режимо pay-конфигураций. */
sealed abstract class MPayMode(override val value: String) extends StringEnumEntry {

  /*** Это продакшен режим работы? */
  def isProd: Boolean

  /** Это тестовый режим работы платёжки? */
  def isTest: Boolean = !isProd

  override final def toString: String = value

}
