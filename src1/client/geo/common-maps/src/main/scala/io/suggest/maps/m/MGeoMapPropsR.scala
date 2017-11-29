package io.suggest.maps.m

import diode.FastEq
import io.suggest.geo.MGeoPoint
import io.suggest.sjs.leaflet.event.{DragEndEvent, Event}
import io.suggest.sjs.leaflet.map.Zoom_t
import io.suggest.spa.OptFastEq
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.11.17 16:17
  * Description: Модель-контейнер пропертисов для компонента карты, которые передаются в LGeoMapR
  * для сборки фактических пропертисов для вызова react-leaflet.
  */

object MGeoMapPropsR {

  /** Поддержка FastEq для инстансов [[MGeoMapPropsR]]. */
  implicit object MGeoMapPropsRFastEq extends FastEq[MGeoMapPropsR] {
    override def eqv(a: MGeoMapPropsR, b: MGeoMapPropsR): Boolean = {
      val P = OptFastEq.Plain
      (a.center          ===* b.center) &&
        (a.zoom           ==* b.zoom) &&
        OptFastEq.OptValueEq.eqv(a.locationFound, b.locationFound) &&
        P.eqv(a.cssClass,     b.cssClass) &&
        P.eqv(a.onDragStart,  b.onDragStart) &&
        P.eqv(a.onDragEnd,    b.onDragEnd)
    }
  }

  def apply(mmap: MMapS): MGeoMapPropsR = {
    apply(
      center          = mmap.center,
      zoom            = mmap.zoom,
      locationFound   = mmap.locationFound,
    )
  }

}


case class MGeoMapPropsR(
                          center          : MGeoPoint,
                          zoom            : Zoom_t,
                          locationFound   : Option[Boolean],
                          cssClass        : Option[String]                           = None,
                          onDragStart     : Option[js.Function1[Event, Unit]]        = None,
                          onDragEnd       : Option[js.Function1[DragEndEvent, Unit]] = None
                        )

