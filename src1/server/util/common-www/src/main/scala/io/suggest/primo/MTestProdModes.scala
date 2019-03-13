package io.suggest.primo

import enumeratum.values.{StringEnum, StringEnumEntry}
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.17 11:24
  * Description: Режимы конфигураций платежных систем.
  * У яндекс-кассы есть "боевой" и "демо/тестовый" режимы.
  * У других -- наверное тоже.
  */

object MTestProdModes extends StringEnum[MTestProdMode] {

  /** Нормальный режим: продакшен, реальная работа с реальными деньгами. */
  case object Production extends MTestProdMode("prod")

  /** Тестовый (доменстрационный) режим работы. Виртуальные деньги. */
  case object Testing extends MTestProdMode("test")


  override def values = findValues

}


/** Класс модели режимо pay-конфигураций. */
sealed abstract class MTestProdMode(override val value: String) extends StringEnumEntry {

  override final def toString = value

}

object MTestProdMode {

  @inline implicit def univEq: UnivEq[MTestProdMode] = UnivEq.derive

}