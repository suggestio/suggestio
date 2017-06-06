package io.suggest.maps.nodes

import boopickle.Default._
import io.suggest.common.geom.d2.Size2di
import io.suggest.model.n2.node.meta.colors.MColors

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.06.17 11:06
  * Description: Модель GeoJSON-пропертисов с инфой об отображении узла-приёмника рекламы, в boopickle.
  */

object MAdvGeoMapNodeProps {

  /** Поддержка boopickle. */
  implicit val mAdvGeoMapNodePropsPickler: Pickler[MAdvGeoMapNodeProps] = {
    implicit val mMapNodeIconInfoP = MMapNodeIconInfo.mMapNodeIconInfoPickler
    implicit val mColorsP = MColors.mColorsPickler
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
                                colors          : MColors,
                                hint            : Option[String]            = None,
                                icon            : Option[MMapNodeIconInfo]  = None
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
