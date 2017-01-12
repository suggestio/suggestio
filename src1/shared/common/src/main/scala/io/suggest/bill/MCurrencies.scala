package io.suggest.bill

import enumeratum._
import io.suggest.primo.IStrId

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.01.17 18:57
  * Description: Модель валют, отвязанная от особенностей жабы и системных локалей.
  */

/** Статическая кроссплатформенная утиль для инстансов модели [[MCurrency]]. */
object MCurrency {

  import boopickle.Default._
  import boopickle.CompositePickler
  import MCurrencies._


  implicit val pickler: CompositePickler[MCurrency] = {
    // TODO scala-2.12 скорее всего можно будет генерить это дело автоматом, без concrete types.
    compositePickler[MCurrency]
      .addConcreteType[RUB.type]
      .addConcreteType[EUR.type]
      .addConcreteType[USD.type]
  }

}


/** Класс для каждого экземпляра модели. */
sealed abstract class MCurrency extends EnumEntry with IStrId {

  /** Строковой международный код валюты. */
  def currencyCode = strId

  /** Код по messages для форматирования цены с указанием данной валюты. */
  def i18nPriceCode = "price." + currencyCode

  /** Вернуть инстанс java currency. */
  def toJavaCurrency = java.util.Currency.getInstance(currencyCode)

}


/** Модель поддеживаемых системой валют. */
object MCurrencies extends Enum[MCurrency] {

  override val values = findValues

  /** Российский рубль. */
  case object RUB extends MCurrency {
    override def strId = "RUB"
    override def toString = super.toString
  }

  /** Евро. */
  case object EUR extends MCurrency {
    override def strId = "EUR"
    override def toString = super.toString
  }

  /** Доллары. */
  case object USD extends MCurrency {
    override def strId = "USD"
    override def toString = super.toString
  }

}
