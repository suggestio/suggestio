package io.suggest.maps.r.rad

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.geo.MGeoPoint
import io.suggest.maps.m.MRad
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.vdom.Implicits._
import RadCircleR.RadCirclePropsValFastEq
import io.suggest.spa.OptFastEq
import io.suggest.spa.OptFastEq.Wrapped
import org.js.react.leaflet.LayerGroup

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.05.17 11:37
  * Description: Компонент базовых элементов карты для Rad: маркеры центра, радиуса и круг заливки.
  */
object RadMapControlsR {

  type Props = ModelProxy[Option[MRad]]


  protected case class State(
                              centerGeoPointOptC      : ReactConnectProxy[Option[MGeoPoint]],
                              radCirclePropsOptC      : ReactConnectProxy[Option[RadCircleR.PropsVal]],
                              radiusGeoPointOptC      : ReactConnectProxy[Option[MGeoPoint]]
                            )


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    // Собрать начальное состояние.
    .initialStateFromProps { mradOptProxy =>
      State(
        centerGeoPointOptC = mradOptProxy.connect { mradOpt =>
          mradOpt.map(_.currentCenter)
        }(OptFastEq.Plain),

        radCirclePropsOptC = mradOptProxy.connect { mradOpt =>
          for (mrad <- mradOpt) yield {
            RadCircleR.PropsVal(
              centerGeoPoint = mrad.currentCenter,
              radiusM        = mrad.circle.radiusM,
              centerDragging = mrad.state.centerDragging.nonEmpty,
              radiusDragging = mrad.state.radiusDragging
            )
          }
        },

        radiusGeoPointOptC = mradOptProxy.connect { mradOpt =>
          for {
            mrad <- mradOpt
            if mrad.state.centerDragging.isEmpty
          } yield {
            mrad.state.radiusMarkerCoords
          }
        }(OptFastEq.Plain)
      )
    }
    // Отрендерить...
    .render_S { s =>
      LayerGroup()(

        // Основной круг для описания слоя:
        s.radCirclePropsOptC { RadCircleR.component.apply },

        // Маркер центра круга.
        // TODO Скрывать маркер центра, если расстояние в пикселях до радиуса < 5
        s.centerGeoPointOptC { DraggablePinMarkerR.component.apply },

        // Маркер радиуса круга. Сделан в виде circle-marker'а.
        s.radiusGeoPointOptC { RadiusMarkerR.component.apply }

      )
    }
    .build

}
