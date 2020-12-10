package io.suggest.bill.price.dsl

import io.suggest.bill.MNameId
import io.suggest.common.empty.EmptyUtil
import io.suggest.geo.{CircleGs, IGeoShape}
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.04.17 15:12
  * Description: Модель описания причины тарификации, увеличения тарифа или чего-то в этом роде.
  */
object MPriceReason {

  implicit def priceReasonFormat: OFormat[MPriceReason] = {
    val fmt = IGeoShape.JsonFormats.minimalFormatter
    import fmt.circle

    (
      (__ \ "r").format[MReasonType] and
      (__ \ "i").formatNullable[Seq[Int]]
        .inmap[Seq[Int]](
          EmptyUtil.opt2ImplEmptyF(Nil),
          x => Option.when( x.nonEmpty )(x)
        ) and
      (__ \ "s").formatNullable[Seq[String]]
        .inmap[Seq[String]](
          EmptyUtil.opt2ImplEmptyF(Nil),
          x => Option.when( x.nonEmpty )(x)
        ) and
      (__ \ "d").formatNullable[Seq[Double]]
        .inmap[Seq[Double]](
          EmptyUtil.opt2ImplEmptyF(Nil),
          x => Option.when( x.nonEmpty )(x)
        ) and
      (__ \ "c").formatNullable[Seq[CircleGs]]
        .inmap[Seq[CircleGs]](
          EmptyUtil.opt2ImplEmptyF(Nil),
          x => Option.when( x.nonEmpty )(x)
        ) and
      (__ \ "n").formatNullable[Seq[MNameId]]
        .inmap[Seq[MNameId]](
          EmptyUtil.opt2ImplEmptyF(Nil),
          x => Option.when( x.nonEmpty )(x)
        )
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MPriceReason] = UnivEq.derive

}


/** Класс описания причины с опциональными дополнительными данными.
  *
  * @param reasonType Тип причины по каталогу причин.
  * @param ints Целочисленный payload'ы, если есть.
  * @param strings Строковой payload'ы, если есть.
  * @param doubles Десятичные payload'ы, если есть.
  * @param geoCircles Гео-круги, если есть.
  */
final case class MPriceReason(
                               reasonType   : MReasonType,
                               ints         : Seq[Int]            = Nil,
                               strings      : Seq[String]         = Nil,
                               doubles      : Seq[Double]         = Nil,
                               geoCircles   : Seq[CircleGs]       = Nil,
                               nameIds      : Seq[MNameId]        = Nil
                             )
