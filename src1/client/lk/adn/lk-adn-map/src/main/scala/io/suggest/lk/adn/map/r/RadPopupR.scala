package io.suggest.lk.adn.map.r

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.geo.MGeoPoint
import io.suggest.maps.u.MapsUtil
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import react.leaflet.popup.PopupR

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.05.17 16:28
  * Description: React-компонент leaflet-попапа, появляющегося при клике по rad-элементам.
  */
object RadPopupR {

  type Props = ModelProxy[Option[PropsVal]]

  case class PropsVal(
                       point  : MGeoPoint
                     )

  implicit object PropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      a.point eq b.point
    }
  }


  protected class Backend($: BackendScope[Props, Unit]) {

    def render(propsProxy: Props): ReactElement = {
      for {
        p <- propsProxy()
      } yield {
        PopupR(
          position = MapsUtil.geoPoint2LatLng(p.point)
        )(
          OptsR( propsProxy )
        )
      }
    }

  }


  val component = ReactComponentB[Props]("RadPopup")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsValProxy: Props) = component(propsValProxy)

}
