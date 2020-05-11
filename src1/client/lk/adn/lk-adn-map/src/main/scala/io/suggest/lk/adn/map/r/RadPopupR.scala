package io.suggest.lk.adn.map.r

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.geo.MGeoPoint
import io.suggest.maps.u.MapsUtil
import io.suggest.react.ReactCommonUtil.Implicits._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.Implicits._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import react.leaflet.popup.{LPopupPropsR, LPopupR}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.05.17 16:28
  * Description: React-компонент leaflet-попапа, появляющегося при клике по rad-элементам.
  */
object RadPopupR {

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]

  case class PropsVal(
                       point  : MGeoPoint
                     )

  implicit object PropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      a.point eq b.point
    }
  }


  protected class Backend($: BackendScope[Props, Unit]) {

    def render(propsProxy: Props): VdomElement = {
      propsProxy().whenDefinedEl { p =>
        LPopupR(
          new LPopupPropsR {
            override val position = MapsUtil.geoPoint2LatLng(p.point)
          }
        )(
          OptsR( propsProxy )
        ): VdomElement
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  private def _apply(propsValProxy: Props) = component(propsValProxy)
  val apply: ReactConnectProps[Props_t] = _apply

}
