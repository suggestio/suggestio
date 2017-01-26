package io.suggest.bill

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.01.17 19:07
  * Description: Клиент-серверная модель данных о стоимости в валюте.
  *
  * Рендер цифры в строку остался на сервере через поле опциональное valueStr.
  * Появилось тут из-за отсутствия DecimalFormat в scala.js и нетривиальности реализации оного.
  *
  * Рендер ценника в целом идёт через Messages, чтобы без привычных проблем с локалями всё рендерить.
  */
object MPrice {

  import boopickle.Default._

  /** Поддержка сериализации. */
  implicit val mPriceBp: Pickler[MPrice] = {
    implicit val currencyP = MCurrency.pickler
    generatePickler[MPrice]
  }


  /** Сгруппировать цены по валютам и просуммировать.
    * Часто надо получить итоговую/итоговые цены для кучи покупок. Вот тут куча цен приходит на вход.
    *
    * @param prices Входная пачка цен.
    * @return Итератор с результирующими ценами.
    */
  def sumPricesByCurrency(prices: Seq[MPrice]): Iterator[MPrice] = {
    if (prices.isEmpty) {
      Iterator.empty
    } else {
      prices
        .groupBy {
          _.currency
        }
        .valuesIterator
        .map { p =>
          MPrice(
            amount   = p.map(_.amount).sum,
            currency = p.head.currency
          )
        }
    }
  }

  /** Вернуть строковое значение цены без какой-либо валюты. */
  def amountStr(m: MPrice): String = {
    m.amountStrOpt
      .getOrElse( "%1.2f".format(m.amount) )
  }


  // Методы с arity=2 для работы с MPrice без поля valueStrOpt.

  def apply2(amount: Amount_t, currency: MCurrency): MPrice = {
    apply(amount, currency)
  }

  def unapply2(m: MPrice) = unapply(m).map { case (amount,curr,_) => (amount,curr) }

}


/**
  * Инстанс данных по цене.
  * @param amount Числовое значение цены.
  * @param currency Валюта.
  * @param amountStrOpt Отформатированное для рендера значение value, если требуется.
  *                    Это поле -- долговременный костыль для scala-js, который наверное будет удалён в будущем.
  */
case class MPrice(
                   amount         : Amount_t,
                   currency       : MCurrency,
                   amountStrOpt   : Option[String] = None
                 )
  extends IMCurrency
{

  def withAmount(value2: Amount_t) = copy(amount = value2, amountStrOpt = None)
  def withValueStrOpt(valueStrOpt2: Option[String]) = copy(amountStrOpt = valueStrOpt2)

  override def toString: String = {
    MPrice.amountStr(this) + currency.currencyCode
  }

}
