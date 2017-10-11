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
import MRadT.MRadTFastEq
import io.suggest.react.ReactCommonUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.01.17 21:55
  * Description: React-компонент для центра круга, описывающего радиус размещения.
  */
object RadR {

  type Props = ModelProxy[Option[MRad]]

  protected case class State(
                              mRadTC                  : ReactConnectProxy[Option[MRadT[_]]],
                              centerPopupC            : ReactConnectProxy[Option[Boolean]],
                              radEnabledPropsC        : ReactConnectProxy[RadEnabledR.PropsVal]
                            )


  protected class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      p().whenDefinedEl { mrad =>

        // Рендер группы слоёв одной пачкой, чтобы можно было всё скопом вернуть наверх.
        LayerGroupR()(

          // Слой с кругом и маркерами управления оными.
          if (mrad.enabled) {
            s.mRadTC { RadMapControlsR.apply }

          } else {
            // !v.enabled -- галочка размещения на карте выключена.
            ReactCommonUtil.VdomNullElement
          },

          // Попап управления центром.
          s.centerPopupC { popupEnabledOpt =>
            val opt = popupEnabledOpt().filter(identity)
            opt.whenDefinedEl { _ =>
              LPopupR(
                new LPopupPropsR {
                  override val position = MapsUtil.geoPoint2LatLng( mrad.currentCenter )
                }
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


  val component = ScalaComponent.builder[Props]("Rad")
    .initialStateFromProps { mradOptProxy =>
      State(
        mRadTC = mradOptProxy.connect(identity),
        centerPopupC = mradOptProxy.connect { mradOpt =>
          mradOpt.map(_.centerPopup)
        },
        radEnabledPropsC = RadEnabledR.radEnabledPropsConn(
          mradOptProxy,
          renderHintAsText = true
        )
      )
    }
    .renderBackend[Backend]
    .build


  def apply(props: Props) = component(props)

}
