package io.suggest.bill

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.Monoid

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
  implicit val mPricePickler: Pickler[MPrice] = {
    implicit val currencyP = MCurrency.pickler
    generatePickler[MPrice]
  }


  object HellImplicits {
    /** Монойд для amount_t, который является double. */
    implicit def AmountMonoid: Monoid[Amount_t] = {
      new Monoid[Amount_t] {
        override def zero: Amount_t = 0d
        override def append(f1: Amount_t, f2: => Amount_t): Amount_t = f1 + f2
      }
    }
  }


  /** Сгруппировать цены по валютам и просуммировать.
    * Часто надо получить итоговую/итоговые цены для кучи покупок. Вот тут куча цен приходит на вход.
    *
    * @param prices Входная пачка цен.
    * @return Итератор с результирующими ценами.
    */
  def sumPricesByCurrency(prices: Seq[MPrice]): Map[MCurrency, MPrice] = {
    if (prices.isEmpty) {
      Map.empty
    } else {
      prices
        .groupBy {
          _.currency
        }
        .mapValues { ps =>
          MPrice(
            amount   = ps.map(_.amount).sum,
            currency = ps.head.currency
          )
        }
    }
  }

  /** Вернуть строковое значение цены без какой-либо валюты. */
  def amountStr(m: MPrice): String = {
    m.amountStrOpt
      .getOrElse( "%1.2f".format(m.amount) )
  }


  /** Сконверить входной список элементов в список цен этих элементов без какой-либо доп.обработки. */
  def toPricesIter(items: TraversableOnce[IMPrice]): Iterator[MPrice] = {
    items
      .toIterator
      .map(_.price)
  }

  /** Объединение toPricesIter() и sumPricesByCurrency().
    * Метод удобен для подсчёта общей стоимости списка каких-то элементов.
    * @param items Исхондая коллекция элементов.
    * @return Карта цен по валютам.
    */
  def toSumPricesByCurrency(items: TraversableOnce[IMPrice]): Map[MCurrency, MPrice] = {
    val prices = toPricesIter(items).toSeq
    sumPricesByCurrency(prices)
  }


  // Методы с arity=2 для работы с MPrice без поля valueStrOpt.

  def apply2(amount: Amount_t, currency: MCurrency): MPrice = {
    apply(amount, currency)
  }

  /** Приведение IPrice к MPrice. */
  def apply(iprice: IPrice): MPrice = {
    iprice match {
      case mprice: MPrice =>
        mprice
      case _ =>
        MPrice(iprice.amount, iprice.currency)
    }
  }

  def unapply2(m: MPrice) = unapply(m).map { case (amount,curr,_) => (amount,curr) }


  /** Поддержка play-json. */
  implicit def mPriceFormat: OFormat[MPrice] = (
    (__ \ "a").format[Amount_t] and
    (__ \ "c").format[MCurrency] and
    (__ \ "f").formatNullable[String]
  )(apply, unlift(unapply))


  implicit def univEq: UnivEq[MPrice] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}

/** Интерфейс для поля amount Double. */
trait IAmount {
  def amount: Amount_t
}

/** Интерфейс к базовым полям моделей, котрорые описывают или включают в себя описание цены/стоимости.
  * Для этого требуется только пара полей: amount и currency.
  */
trait IPrice
  extends IAmount
  with IMCurrency


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
  extends IPrice
{

  def withAmount(value2: Amount_t) = copy(amount = value2, amountStrOpt = None)
  def withAmountStrOpt(amountStrOpt2: Option[String]) = copy(amountStrOpt = amountStrOpt2)

  override def toString: String = {
    MPrice.amountStr(this) + currency.currencyCode
  }

  /** Домножить amount на какой-то коэффициент. */
  def multiplifiedBy(mult: Double) = withAmount(amount * mult)
  def *(mult: Double) = multiplifiedBy(mult)

  /** Увеличить (уменьшить) объем средств на указанное число. */
  def plusAmount(plusAmount: Amount_t) = withAmount(amount + plusAmount)

  /** Нормировать значение amount согласно экспоненте валюты.
    * Иными словами, отбросить доли копеек и прочего.
    */
  def normalizeAmountByExponent: MPrice = {
    val expMult = currency.centsInUnit_d
    val amount2 = (amount * expMult).toLong.toDouble / expMult
    if (amount2 != amount) {
      withAmount(amount2)
    } else {
      this
    }
  }

}


/**
  * Интерфейс к экземплярам моделей к полю с ценой.
  * Долго жил в mbill2.
  */
trait IMPrice {

  /** Цена: значение + валюта. */
  def price: MPrice

}
