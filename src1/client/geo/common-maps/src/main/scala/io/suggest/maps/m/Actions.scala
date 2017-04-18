package io.suggest.maps.m

import io.suggest.geo.{IGeoPointField, MGeoPoint}
import io.suggest.sjs.common.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.04.17 22:14
  * Description: Экшены для react-компонентов гео.карт.
  *
  * Трейты без sealed, т.к. они потом гуляют по зависимым формам и circuit'ам.
  */

trait IMapsAction extends DAction

/** Интерфейс для сообщений выставления центра. */
trait ISetMapCenterForPopup extends IMapsAction with IGeoPointField


trait ISetMapCenter extends IMapsAction with IGeoPointField

/** Экшен выставления центра карты на указанную гео-точку. */
case class HandleLocationFound(override val geoPoint: MGeoPoint) extends ISetMapCenter


trait IHandleMapPopupClose extends IMapsAction

/** Команда среагировать на сокрытие произвольного попапа карты на стороне leaflet. */
case object HandleMapPopupClose extends IHandleMapPopupClose
