package io.suggest.lk.adv.geo.r.geo.adv

import diode.react.ModelProxy
import io.suggest.lk.adv.geo.m.MGeoAdvs
import io.suggest.lk.adv.geo.u.LkAdvGeoFormUtil
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import japgolly.scalajs.react.vdom.prefix_<^._
import react.leaflet.popup.PopupR

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.12.16 16:13
  * Description: React-компонент содержимого попапа над георазмещением на карте.
  */
object GeoAdvExistPopupR {

  type Props = ModelProxy[MGeoAdvs]


  /** Рендерер содержимого попапа. */
  class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): ReactElement = {
      val p0 = p()
      val elOpt = for {
        popResp   <- p0.popupResp.toOption
        popState  <- p0.popupState
      } yield {
        PopupR(
          position = LkAdvGeoFormUtil.geoPoint2LatLng( popState.position )
        )(
          <.ul(
            "TODO"
          )
        )
      }
      elOpt.orNull
    }

  }


  val component = ReactComponentB[Props]("GeoAdvExistPop")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(mGeoAdvs: Props) = component(mGeoAdvs)

}
