package io.suggest.maps.r.rad

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.geo.MGeoPoint
import io.suggest.maps.m._
import io.suggest.maps.u.MapsUtil
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import react.leaflet.layer.LayerGroupR
import react.leaflet.popup.PopupR
import io.suggest.sjs.common.spa.OptFastEq.Wrapped
import RadCircleR.RadCirclePropsValFastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.01.17 21:55
  * Description: React-компонент для центра круга, описывающего радиус размещения.
  */
object RadR {

  type Props = ModelProxy[Option[MRad]]

  protected case class State(
                              centerGeoPointOptC      : ReactConnectProxy[Option[MGeoPoint]],
                              centerPopupC            : ReactConnectProxy[Option[Boolean]],
                              radEnabledPropsC        : ReactConnectProxy[RadEnabledR.PropsVal],
                              radCirclePropsOptC      : ReactConnectProxy[Option[RadCircleR.PropsVal]],
                              radiusGeoPointOptC      : ReactConnectProxy[Option[MGeoPoint]]
                            )


  protected class Backend($: BackendScope[Props, State]) extends RadBackendHelper($) {

    def render(p: Props, s: State): ReactElement = {
      for {
        mrad <- p()
      } yield {

        // Рендер группы слоёв одной пачкой, чтобы можно было всё скопом вернуть наверх.
        LayerGroupR()(

          // Слой с кругом и маркерами управления оными.
          if (mrad.enabled) {
            LayerGroupR()(
              // Основной круг для описания слоя:
              s.radCirclePropsOptC { RadCircleR.apply },

              // Маркер центра круга.
              // TODO Скрывать маркер центра, если расстояние в пикселях до радиуса < 5
              s.centerGeoPointOptC { DraggablePinMarkerR.apply },

              // Маркер радиуса круга. Сделан в виде circle-marker'а.
              s.radiusGeoPointOptC { RadiusMarkerR.apply }

            )
          } else {
            // !v.enabled -- галочка размещения на карте выключена.
            null
          },

          // Попап управления центром.
          s.centerPopupC { popupEnabledOpt =>
            for (isEnabled <- popupEnabledOpt() if isEnabled) yield {
              PopupR(
                position = MapsUtil.geoPoint2LatLng( mrad.currentCenter )
              )(
                <.div(
                  s.radEnabledPropsC( RadEnabledR.apply )
                )
              )
            }
          }

        )

      }
    }

  }


  val component = ReactComponentB[Props]("Rad")
    .initialState_P { mradOptProxy =>
      State(
        centerPopupC = mradOptProxy.connect { mradOpt =>
          mradOpt.map(_.centerPopup)
        },
        radEnabledPropsC = RadEnabledR.radEnabledPropsConn(mradOptProxy, renderHintAsText = true),
        centerGeoPointOptC = mradOptProxy.connect { mradOpt =>
          mradOpt.map(_.currentCenter)
        },
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
        }
      )
    }
    .renderBackend[Backend]
    .build


  def apply(props: Props) = component(props)

}
