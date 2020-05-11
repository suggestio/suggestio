package io.suggest.bill

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq._
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.01.17 18:57
  * Description: Модель валют, поддерживаемых системой s.io.
  */

object MCurrencies extends StringEnum[MCurrency] {

  /** Российский рубль.
    * @see [[https://en.wikipedia.org/wiki/Russian_ruble]]
    */
  case object RUB extends MCurrency("RUB") {
    override def htmlSymbol = "&#8381;"
    override def symbol     = "₽"
    override def iso4217    = 643
    override def sioPaymentAmountMinUnits: Amount_t = 100
  }

  /** Евро.
    * @see [[https://en.wikipedia.org/wiki/Euro]]
    */
  case object EUR extends MCurrency("EUR") {
    override def htmlSymbol = "&#8364;"
    override def symbol     = "€"
    override def iso4217    = 978
    override def sioPaymentAmountMinUnits: Amount_t = 5
  }

  /** Доллары США.
    * @see [[https://en.wikipedia.org/wiki/United_States_dollar]]
    */
  case object USD extends MCurrency("USD") {
    override def htmlSymbol = symbol
    override def symbol     = "$"
    override def iso4217    = 840
    override def sioPaymentAmountMinUnits: Amount_t = 5
  }

  def withIso4217Option(code: Int) = values.find(_.iso4217 ==* code)

  override val values = findValues

  def default: MCurrency = values.head


  /** Список моделей "цен" (или чего-то ещё) превращаем в карту цен по валютами.
    * Если валюты повторяются, то будет неправильная мапа на выходе.
    *
    * @param prices Исходные валютные модели.
    * @return Карта исходных моделей по валютам БЕЗ учёта дубликатов.
    */
  def hardMapByCurrency[T <: IMCurrency](prices: IterableOnce[T]): Map[MCurrency, T] = {
    prices
      .iterator
      .map { p => p.currency -> p }
      .toMap
  }

  def toCurrenciesIter(items: IterableOnce[IMCurrency]): Iterator[MCurrency] = {
    items
      .iterator
      .map(_.currency)
  }

}


/** Класс для каждого экземпляра модели. */
sealed abstract class MCurrency(override val value: String) extends StringEnumEntry {

  override final def toString = value

  /** Строковой международный код валюты. */
  final def currencyCode = value

  /** Код по messages для форматирования цены с указанием данной валюты. */
  def i18nPriceCode = "price." + currencyCode

  /** Код по messages с полным локализвоанным названием валюты. */
  def currencyNameI18n = "currency." + currencyCode

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
    * Доли рубля вычислялись так: Math.pow(10, -exponent)
    */
  def minAmount: Amount_t = 1L

  /** Количество центов в долларе, копеек в рубле в виде Double.
    * Math.pow() возвращает Double, и иногда нет смысла конвертировать в Int, используется этот метод. */
  def centsInUnit: Amount_t =
    Math.pow(10, exponent).toLong

  /** Минимальный размер платежа в данной валюте на suggest.io. */
  def sioPaymentAmountMinUnits: Amount_t
  final def sioPaymentAmountMin: Amount_t = sioPaymentAmountMinUnits * centsInUnit

}

/** Статическая кроссплатформенная утиль для инстансов модели [[MCurrency]]. */
object MCurrency {

  import boopickle.CompositePickler

  implicit val pickler: CompositePickler[MCurrency] = {
    import MCurrencies._
    import boopickle.Default._
    // TODO scala-2.12 скорее всего можно будет генерить это дело автоматом, без concrete types.
    compositePickler[MCurrency]
      .addConcreteType[RUB.type]
      .addConcreteType[EUR.type]
      .addConcreteType[USD.type]
  }

  @inline implicit def univEq: UnivEq[MCurrency] = UnivEq.derive

  /** Поддержка play-json. */
  implicit def mCurrencyFormat: Format[MCurrency] =
    EnumeratumUtil.valueEnumEntryFormat( MCurrencies )

}



/** Интерфейс для обязательного поля currency. */
trait IMCurrency {
  def currency: MCurrency
}
