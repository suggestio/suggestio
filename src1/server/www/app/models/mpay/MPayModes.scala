package models.mpay

import enumeratum._
import io.suggest.primo.IStrId

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.17 11:24
  * Description: Режимы конфигураций платежных систем.
  * У яндекс-кассы есть "боевой" и "демо/тестовый" режимы.
  * У других -- наверное тоже.
  */

object MPayModes extends Enum[MPayMode] {

  /** Нормальный режим: продакшен, реальная работа с реальными деньгами. */
  case object Production extends MPayMode {
    override def isProd   = true
    override def toString = "prod"
  }

  /** Тестовый (доменстрационный) режим работы. Виртуальные деньги. */
  case object Testing extends MPayMode {
    override def isProd   = false
    override def toString = "test"
  }


  override def values = findValues

}


/** Класс модели режимо pay-конфигураций. */
sealed abstract class MPayMode extends EnumEntry with IStrId {

  /*** Это продакшен режим работы? */
  def isProd: Boolean

  /** Это тестовый режим работы платёжки? */
  def isTest: Boolean = !isProd

  override final def strId = toString

}

