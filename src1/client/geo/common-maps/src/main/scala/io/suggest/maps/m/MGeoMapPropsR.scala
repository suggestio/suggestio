package io.suggest.maps.m

import diode.FastEq
import io.suggest.geo.MGeoPoint
import io.suggest.sjs.leaflet.event.{DragEndEvent, Event}
import io.suggest.sjs.leaflet.map.{IWhenReadyArgs, Zoom_t}
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
      val V = OptFastEq.OptValueEq
      (a.mapS ===* b.mapS) &&
      (a.animated ==* b.animated) &&
      P.eqv(a.cssClass,     b.cssClass) &&
      V.eqv(a.trackWndResize, b.trackWndResize) &&
      P.eqv(a.whenReady,    b.whenReady) &&
      P.eqv(a.onDragStart,  b.onDragStart) &&
      P.eqv(a.onDragEnd,    b.onDragEnd)
    }
  }

}


case class MGeoMapPropsR(
                          mapS            : MMapS,
                          animated        : Boolean                                       = true,
                          cssClass        : Option[String]                                = None,
                          trackWndResize  : Option[Boolean]                               = None,
                          whenReady       : Option[js.Function1[IWhenReadyArgs, Unit]]    = None,
                          onDragStart     : Option[js.Function1[Event, Unit]]             = None,
                          onDragEnd       : Option[js.Function1[DragEndEvent, Unit]]      = None,
                        )

