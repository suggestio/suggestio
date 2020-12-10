package io.suggest.bill

import io.suggest.bill.price.dsl.PriceDsl
import io.suggest.common.empty.EmptyUtil
import io.suggest.scalaz.ZTreeUtil._
import japgolly.univeq.UnivEq
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.01.17 18:44
  * Description: Кросс-платформенная модель ответа на запрос рассчёта стоимости чего-либо (размещения).
  *
  * Модель идёт на замену server-only модели MAdvPricing, слишком сильно зависящей от java.util.Currency
  * и особенностей старого биллинга.
  */

object MGetPriceResp {

  @inline implicit def univEq: UnivEq[MGetPriceResp] = UnivEq.derive

  implicit def getPriceRespJson: OFormat[MGetPriceResp] = (
    (__ \ "p").formatNullable[Seq[MPrice]]
      .inmap[Seq[MPrice]](
        EmptyUtil.opt2ImplEmptyF(Nil),
        x => Option.when(x.nonEmpty)(x)
      ) and
    (__ \ "d").formatNullable[Tree[PriceDsl]]
  )(apply, unlift(unapply))

  def prices = GenLens[MGetPriceResp]( _.prices )
  def priceDsl = GenLens[MGetPriceResp]( _.priceDsl )

}


/**
  * Класс модели ответа на запрос рассчёта стоимости [размещения].
  * @param prices Данные по стоимостям.
  *               Iterable для упрощения некоторого кода, было изначально Seq[].
  *               Ситуация, когда несколько валют, довольно маловероятна.
  * @param priceDsl Терм PriceDsl, если есть.
  */
case class MGetPriceResp(
                          prices    : Seq[MPrice],
                          priceDsl  : Option[Tree[PriceDsl]]   = None
                        ) {

  override def toString: String = {
    prices.mkString("$[", ",", "]")
  }

}
