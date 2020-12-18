package io.suggest.maps.r.rad

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.maps.m._
import io.suggest.maps.u.MapsUtil
import io.suggest.react.ReactCommonUtil.Implicits._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import react.leaflet.layer.LayerGroupR
import react.leaflet.popup.{LPopupPropsR, LPopupR}
import io.suggest.spa.OptFastEq.Wrapped
import io.suggest.common.empty.OptionUtil
import io.suggest.react.ReactCommonUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.01.17 21:55
  * Description: React-компонент для центра круга, описывающего радиус размещения.
  */
object RadR {

  type Props_t = MAdvGeoS
  type Props = ModelProxy[Props_t]

  protected case class State(
                              centerPopupC            : ReactConnectProxy[Option[Boolean]],
                              radEnabledPropsC        : ReactConnectProxy[RadEnabledR.PropsVal]
                            )


  protected class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      p().rad.whenDefinedEl { mrad =>

        // Рендер группы слоёв одной пачкой, чтобы можно было всё скопом вернуть наверх.
        LayerGroupR()(

          // Слой с кругом и маркерами управления оными.
          {
            lazy val radEl = RadMapControlsR.component( p.zoom(_.rad) )
            ReactCommonUtil.maybeEl( mrad.enabled )( radEl )
          },

          // Попап управления центром.
          {
            lazy val popupContent = <.div(
              s.radEnabledPropsC( RadEnabledR.component.apply )
            )
            s.centerPopupC { popupEnabledOpt =>
              popupEnabledOpt
                .value
                .filter( identity )
                .whenDefinedEl { _ =>
                  LPopupR(
                    new LPopupPropsR {
                      override val position = MapsUtil.geoPoint2LatLng( mrad.currentCenter )
                    }
                  )(
                    popupContent,
                  )
                }
            }
          },

        )

      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { mradOptProxy =>
      State(
        centerPopupC = mradOptProxy.connect { props =>
          OptionUtil.SomeBool( props.radPopup )
        },
        radEnabledPropsC = RadEnabledR.radEnabledPropsConn(
          mradOptProxy.zoom(_.rad),
          renderHintAsText = true
        )
      )
    }
    .renderBackend[Backend]
    .build

}
