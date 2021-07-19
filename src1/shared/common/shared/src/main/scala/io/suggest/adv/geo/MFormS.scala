package io.suggest.adv.geo

import io.suggest.common.empty.EmptyUtil
import io.suggest.common.tags.edit.MTagsEditProps
import io.suggest.dt.MAdvPeriod
import io.suggest.geo.{CircleGs, IGeoShape}
import io.suggest.maps.MMapProps
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.12.16 16:53
  * Description: Модели состояния формы георазмещения, расшаренные между клиентом и сервером.
  *
  * Бинарщина быстрая и обфусцированная даже на предмет типов и имён полей.
  *
  * 2016.12.28: Было решено, что эта модель должна быть более эфемерна, чем было изначально.
  * Т.е. должна быть js-only модель MRoot, с полями, которые соответствуют компонентам,
  * MRoot при необходимости может экспортировать одноразовые инстансы со своими кусками в виде вот этой
  * клиент-серверной модели. Это поможет выкинуть поле adId, сгуппировать разные разбросанные модели по полям.
  */

object MFormS {

  def TZ_OFFSET_IGNORE = Int.MinValue

  implicit def advGeoFormJson: OFormat[MFormS] = {
    val gsSer = IGeoShape.JsonFormats.minimalFormatter
    import gsSer.circle

    (
      (__ \ "p").format[MMapProps] and
      (__ \ "m").format[Boolean] and
      (__ \ "e").formatNullable[Boolean] and
      (__ \ "r").formatNullable[RcvrsMap_t]
        .inmap[RcvrsMap_t]( EmptyUtil.opt2ImplEmptyF(Map.empty), x => Option.when(x.nonEmpty)(x) ) and
      (__ \ "t").format[MTagsEditProps] and
      (__ \ "d").format[MAdvPeriod] and
      (__ \ "c").formatNullable[CircleGs] and
      (__ \ "z").format[Int]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MFormS] = UnivEq.derive

  def radCircle = GenLens[MFormS](_.radCircle)
  def rcvrsMap = GenLens[MFormS](_.rcvrsMap)

}


/** Корневая модель состояния формы георазмещения на базе diode circuit.
  * Этот класс пошарен между сервером и клиентом, поэтому
  *
  * param mapState id георазмещаемой рекламной карточки.
  * param existCircles Текущие кружки, если есть.
  *
  * @param tzOffsetMinutes С сервера на клиент -- вообще любое число (игнорится клиентом).
  *                        На клиенте: вычисляется на лету и отсылается на сервер.
  *                        Т.е. оно типа обязательное с клиента на сервер, но ненужное с сервера на клиент.
  */
case class MFormS(
                   mapProps         : MMapProps,
                   onMainScreen     : Boolean,
                   adv4freeChecked  : Option[Boolean]   = None,
                   rcvrsMap         : RcvrsMap_t        = Map.empty,
                   tagsEdit         : MTagsEditProps    = MTagsEditProps.empty,
                   datePeriod       : MAdvPeriod        = MAdvPeriod.default,
                   radCircle        : Option[CircleGs]  = None,
                   tzOffsetMinutes  : Int               = 180,
                 )
