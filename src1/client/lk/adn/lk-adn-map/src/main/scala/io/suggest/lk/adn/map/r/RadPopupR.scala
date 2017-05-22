package io.suggest.lk.adn.map.r

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.adn.mapf.opts.MLamOpts
import io.suggest.geo.MGeoPoint
import io.suggest.maps.u.MapsUtil
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import japgolly.scalajs.react.vdom.prefix_<^._
import react.leaflet.popup.PopupR
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.05.17 16:28
  * Description: React-компонент leaflet-попапа, появляющегося при клике по rad-элементам.
  */
object RadPopupR {

  type Props = ModelProxy[Option[PropsVal]]

  case class PropsVal(
                       point  : MGeoPoint,
                       optsC  : ReactConnectProxy[MLamOpts]
                     )

  implicit object PropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.point eq b.point) &&
        (a.optsC eq b.optsC)
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
          p.optsC { OptsR.apply }
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
