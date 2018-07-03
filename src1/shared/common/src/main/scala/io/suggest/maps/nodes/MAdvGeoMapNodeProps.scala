package io.suggest.maps.nodes

import io.suggest.common.empty.EmptyUtil
import io.suggest.common.geom.d2.MSize2di
import io.suggest.model.n2.node.meta.colors.MColors
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.06.17 11:06
  * Description: Модель GeoJSON-пропертисов с инфой об отображении узла-приёмника рекламы, в boopickle.
  */

object MAdvGeoMapNodeProps {

  implicit def MAdvGeoMapNodePropsFormat: OFormat[MAdvGeoMapNodeProps] = (
    (__ \ "n").format[String] and
    (__ \ "c").formatNullable[MColors]
      .inmap[MColors]( EmptyUtil.opt2ImplMEmptyF(MColors), EmptyUtil.implEmpty2OptF) and
    (__ \ "h").formatNullable[String] and
    (__ \ "i").formatNullable[MMapNodeIconInfo]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MAdvGeoMapNodeProps] = UnivEq.derive

}

/** Модель пропертей узлов, отображаемых на карте.
  *
  * @param nodeId id узла для возможности запроса попапа или какие-то ещё действия производить.
  * @param hint Подсказка при наведении на узел.
  * @param icon Логотип узла, отображаемый на карте.
  */
case class MAdvGeoMapNodeProps(
                                nodeId          : String,
                                colors          : MColors                   = MColors.empty,
                                hint            : Option[String]            = None,
                                icon            : Option[MMapNodeIconInfo]  = None
                              )



object MMapNodeIconInfo {

  implicit def mMapNodeIconInfoFormat: OFormat[MMapNodeIconInfo] = (
    (__ \ "u").format[String] and
    (__ \ "wh").format[MSize2di]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MMapNodeIconInfo] = UnivEq.derive

}

case class MMapNodeIconInfo(
                             url     : String,
                             wh      : MSize2di
                           )
