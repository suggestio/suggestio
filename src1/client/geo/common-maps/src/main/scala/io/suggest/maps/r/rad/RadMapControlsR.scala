package io.suggest.maps.r.rad

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.geo.MGeoPoint
import io.suggest.maps.m.MRadT
import japgolly.scalajs.react.ReactComponentB
import react.leaflet.layer.LayerGroupR
import io.suggest.sjs.common.spa.OptFastEq
import io.suggest.sjs.common.spa.OptFastEq.Wrapped
import RadCircleR.RadCirclePropsValFastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.05.17 11:37
  * Description: Компонент базовых элементов карты для Rad: маркеры центра, радиуса и круг заливки.
  */
object RadMapControlsR {

  type Props = ModelProxy[Option[MRadT[_]]]


  protected case class State(
                              centerGeoPointOptC      : ReactConnectProxy[Option[MGeoPoint]],
                              radCirclePropsOptC      : ReactConnectProxy[Option[RadCircleR.PropsVal]],
                              radiusGeoPointOptC      : ReactConnectProxy[Option[MGeoPoint]]
                            )


  val component = ReactComponentB[Props]("Rad")
    // Собрать начальное состояние.
    .initialState_P { mradOptProxy =>
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
      LayerGroupR()(

        // Основной круг для описания слоя:
        s.radCirclePropsOptC { RadCircleR.apply },

        // Маркер центра круга.
        // TODO Скрывать маркер центра, если расстояние в пикселях до радиуса < 5
        s.centerGeoPointOptC { DraggablePinMarkerR.apply },

        // Маркер радиуса круга. Сделан в виде circle-marker'а.
        s.radiusGeoPointOptC { RadiusMarkerR.apply }

      )
    }
    .build


  def apply(props: Props) = component(props)

}
