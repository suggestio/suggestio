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
  implicit val pickler: Pickler[MPrice] = {
    implicit val currencyP = MCurrency.pickler
    generatePickler[MPrice]
  }

  /** Вернуть строковое значение цены без какой-либо валюты. */
  def valueStr(m: MPrice): String = {
    m.valueStrOpt
      .getOrElse( "%1.2f".format(m.value) )
  }

}


/**
  * Инстанс данных по цене.
  * @param value Числовое значение цены.
  * @param currency Валюта.
  * @param valueStrOpt Отформатированное для рендера значение value, если требуется.
  */
case class MPrice(
                   value          : Double,
                   currency       : MCurrency,
                   valueStrOpt    : Option[String] = None
                 )
{

  override def toString: String = {
    MPrice.valueStr(this) + currency.currencyCode
  }

}