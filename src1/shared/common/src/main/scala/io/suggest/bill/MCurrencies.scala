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

  /** Символ в HTML. */
  def htmlSymbol: String

  /** Символ валюты в виде строки. */
  def symbol: String

  /** Код валюты согласно ISO 4217.
    * @see [[https://en.wikipedia.org/wiki/ISO_4217]]
    */
  def iso4217: Int

  /** Копейки/центы после десятичной запятой. */
  def exponent: Int = 2

  /** Минимальный значимый объем средств.
    * Доли центов и копеек не имеют никакого смысла.
    */
  def minAmount: Amount_t = Math.pow(10, -exponent)

  /** Количество центов в долларе, копеек в рубле. */
  def centsInUnit: Int = centsInUnit_d.toInt

  /** Количество центов в долларе, копеек в рубле в виде Double.
    * Math.pow() возвращает Double, и иногда нет смысла конвертировать в Int, используется этот метод. */
  def centsInUnit_d: Double = Math.pow(10, exponent)

}


/** Модель поддеживаемых системой валют. */
object MCurrencies extends Enum[MCurrency] {

  override val values = findValues

  def default: MCurrency = values.head

  /** Российский рубль.
    * @see [[https://en.wikipedia.org/wiki/Russian_ruble]]
    */
  case object RUB extends MCurrency {
    override def strId      = "RUB"
    override def toString   = super.toString
    override def htmlSymbol = "&#8381;"
    override def symbol     = "₽"
    override def iso4217    = 643
  }

  /** Евро.
    * @see [[https://en.wikipedia.org/wiki/Euro]]
    */
  case object EUR extends MCurrency {
    override def strId      = "EUR"
    override def toString   = super.toString
    override def htmlSymbol = "&#8364;"
    override def symbol     = "€"
    override def iso4217    = 978
  }

  /** Доллары США.
    * @see [[https://en.wikipedia.org/wiki/United_States_dollar]]
    */
  case object USD extends MCurrency {
    override def strId      = "USD"
    override def toString   = super.toString
    override def htmlSymbol = symbol
    override def symbol     = "$"
    override def iso4217    = 840
  }

  def withIso4217Option(code: Int) = values.find(_.iso4217 == code)


  /** Список моделей "цен" (или чего-то ещё) превращаем в карту цен по валютами.
    * Если валюты повторяются, то будет неправильная мапа на выходе.
    *
    * @param prices Исходные валютные модели.
    * @return Карта исходных моделей по валютам БЕЗ учёта дубликатов.
    */
  def hardMapByCurrency[T <: IMCurrency](prices: TraversableOnce[T]): Map[MCurrency, T] = {
    prices.toIterator
      .map { p => p.currency -> p }
      .toMap
  }

  def toCurrenciesIter(items: TraversableOnce[IMCurrency]): Iterator[MCurrency] = {
    items.toIterator
      .map(_.currency)
  }

}


/** Интерфейс для обязательного поля currency. */
trait IMCurrency {
  def currency: MCurrency
}
