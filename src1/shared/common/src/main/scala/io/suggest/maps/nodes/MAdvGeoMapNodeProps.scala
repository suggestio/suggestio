package io.suggest.maps.nodes

import boopickle.Default._
import io.suggest.common.geom.d2.Size2di

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.06.17 11:06
  * Description: Модель GeoJSON-пропертисов с инфой об отображении узла-приёмника рекламы, в boopickle.
  */

object MAdvGeoMapNodeProps {

  /** Поддержка boopickle. */
  implicit val mAdvGeoMapNodePropsPickler: Pickler[MAdvGeoMapNodeProps] = {
    implicit val mMapNodeIconInfo = MMapNodeIconInfo.mMapNodeIconInfoPickler
    generatePickler[MAdvGeoMapNodeProps]
  }

}

/** Модель пропертей узлов, отображаемых на карте.
  *
  * @param nodeId id узла для возможности запроса попапа или какие-то ещё действия производить.
  * @param hint Подсказка при наведении на узел.
  * @param icon Логотип узла, отображаемый на карте.
  */
case class MAdvGeoMapNodeProps(
                                nodeId          : String,
                                hint            : Option[String],
                                icon            : Option[MMapNodeIconInfo]  = None,
                                bgColor         : Option[String]            = None,
                                circleRadiusM   : Option[Double]            = None
                              )



object MMapNodeIconInfo {

  /** Поддержка boopickle. */
  implicit val mMapNodeIconInfoPickler: Pickler[MMapNodeIconInfo] = {
    implicit val size2diP = Size2di.size2diPickler
    generatePickler[MMapNodeIconInfo]
  }

}

case class MMapNodeIconInfo(
  url     : String,
  wh      : Size2di
)
