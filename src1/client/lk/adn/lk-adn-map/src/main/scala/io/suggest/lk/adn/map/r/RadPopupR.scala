package io.suggest.lk.adn.map.r

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.geo.MGeoPoint
import io.suggest.maps.m.MAdvGeoS
import io.suggest.maps.u.MapsUtil
import io.suggest.react.ReactCommonUtil.Implicits._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.Implicits._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import org.js.react.leaflet.{Popup, PopupProps}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.05.17 16:28
  * Description: React-компонент leaflet-попапа, появляющегося при клике по rad-элементам.
  */
final class RadPopupR(
                       optsR: OptsR,
                     ) {

  type Props_t = MAdvGeoS
  type Props = ModelProxy[Props_t]

  protected case class State(
                              shownAtOrNullC : ReactConnectProxy[MGeoPoint],
                            )

  protected class Backend($: BackendScope[Props, State]) {

    def render(propsProxy: Props, s: State): VdomElement = {
      lazy val opts = optsR.component( propsProxy )

      s.shownAtOrNullC { shownAtOrNullProxy =>
        Option( shownAtOrNullProxy() ).whenDefinedEl { p =>
          Popup(
            new PopupProps {
              override val position = MapsUtil.geoPoint2LatLng( p )
            }
          )(
            opts,
          )
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        shownAtOrNullC = propsProxy.connect { props =>
          props.rad
            .filter( _ => props.radPopup )
            .map(_.circle.center)
            .orNull
        },
      )
    }
    .renderBackend[Backend]
    .build

}
