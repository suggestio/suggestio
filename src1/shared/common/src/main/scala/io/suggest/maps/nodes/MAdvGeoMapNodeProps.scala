package io.suggest.maps.nodes

import boopickle.Default._
import io.suggest.common.geom.d2.MSize2di
import io.suggest.model.n2.node.meta.colors.MColors
import japgolly.univeq.UnivEq

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
                                colors          : MColors,
                                hint            : Option[String]            = None,
                                icon            : Option[MMapNodeIconInfo]  = None
                              )



object MMapNodeIconInfo {

  /** Поддержка boopickle. */
  implicit val mMapNodeIconInfoPickler: Pickler[MMapNodeIconInfo] = {
    implicit val size2diP = MSize2di.size2diPickler
    generatePickler[MMapNodeIconInfo]
  }

  implicit def univEq: UnivEq[MMapNodeIconInfo] = UnivEq.derive

}

case class MMapNodeIconInfo(
  url     : String,
  wh      : MSize2di
)
