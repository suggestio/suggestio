package io.suggest.lk.adv.geo.r.geo.adv

import diode.react.ModelProxy
import io.suggest.adv.geo.{InGeoTag, OnMainScreen}
import io.suggest.lk.adv.geo.m.MGeoAdvs
import io.suggest.lk.adv.geo.u.LkAdvGeoFormUtil
import io.suggest.react.r.RangeYmdR
import io.suggest.lk.vm.LkMessagesWindow.Messages
import io.suggest.common.html.HtmlConstants._
import io.suggest.css.Css
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


  /** Тег с префиксом тега. */
  lazy val _tagPrefix = <.span(
    ^.`class` := Css.Colors.LIGHT_GRAY,
    ^.title   := Messages("GeoTag"),
    TAG_PREFIX
  )

  /** Тег с постфиком-галочкой. */
  lazy val _onlineNow = <.span(
    ^.title := Messages("_adv.Online.now"),
    CHECKMARK
  )


  /** Рендерер содержимого попапа. */
  class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): ReactElement = {
      val p0 = p()
      val elOpt = for {
        popResp   <- p0.popupResp.toOption
        popState  <- p0.popupState
      } yield {
        PopupR(
          position = LkAdvGeoFormUtil.geoPoint2LatLng( popState.open.geoPoint )
        )(
          <.ul(
            for (row <- popResp.rows) yield {
              <.li(
                // Рендер диапазона дат, если указан...
                for (rangeYmd <- row.dateRange) yield {
                  RangeYmdR(
                    RangeYmdR.Props(
                      capFirst = true,
                      rangeYmd = rangeYmd,
                      Messages = Messages
                    )
                  )
                },

                SPACE,

                // Рендер строкой всех item'ов в текущем диапазоне.
                for (itm <- row.items) yield {
                  <.span(
                    ^.key := itm.itemId,
                    itm.payload match {
                      case OnMainScreen   => Messages("Main.screen")
                      case InGeoTag(face) => <.span(
                        _tagPrefix,
                        face
                      )
                    },
                    itm.isOnlineNow ?= _onlineNow,
                    COMMA, SPACE
                  )
                }

              )
            }
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
