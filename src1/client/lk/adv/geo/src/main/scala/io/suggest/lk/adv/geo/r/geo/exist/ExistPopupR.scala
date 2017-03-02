package io.suggest.lk.adv.geo.r.geo.exist

import diode.react.ModelProxy
import io.suggest.adv.geo.{InGeoTag, OnMainScreen}
import io.suggest.common.html.HtmlConstants._
import io.suggest.css.Css
import io.suggest.lk.adv.geo.m.MGeoAdvs
import io.suggest.lk.adv.geo.u.LkAdvGeoFormUtil
import io.suggest.react.r.RangeYmdR
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import io.suggest.sjs.common.i18n.Messages
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import react.leaflet.popup.PopupR

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.12.16 16:13
  * Description: React-компонент содержимого попапа над георазмещением на карте.
  */
object ExistPopupR {

  type Props = ModelProxy[MGeoAdvs]


  /** Тег с префиксом тега. */
  private lazy val _tagPrefix = {
    <.span(
      ^.`class` := Css.Colors.LIGHT_GRAY,
      ^.title   := Messages("GeoTag"),
      TAG_PREFIX
    )
  }

  /** Тег с постфиком-галочкой. */
  private lazy val _onlineNow = {
    <.span(
      ^.title := Messages("_adv.Online.now"),
      CHECKMARK
    )
  }


  /** Рендерер содержимого попапа. */
  class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): ReactElement = {
      val p0 = p()

      for {
        popResp   <- p0.popupResp.toOption
        popState  <- p0.popupState
      } yield {
        PopupR(
          position = LkAdvGeoFormUtil.geoPoint2LatLng( popState.open.geoPoint )
        )(
          <.ul(
            for (row <- popResp.rows) yield {
              <.li(
                ^.key := row.dateRange.toString,

                // Рендер диапазона дат, если указан...
                row.dateRange.nonEmpty ?= RangeYmdR(
                  RangeYmdR.Props(
                    capFirst = true,
                    rangeYmdOpt = row.dateRange
                  )
                ),

                SPACE,

                // Рендер строкой всех item'ов в текущем диапазоне.
                for (itm <- row.items) yield {
                  <.span(
                    ^.key := itm.itemId,
                    itm.payload match {
                      case OnMainScreen   =>
                        Messages("Main.screen")
                      case InGeoTag(face) =>
                        <.span(
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

    }

  }


  val component = ReactComponentB[Props]("GeoAdvExistPop")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(mGeoAdvs: Props) = component(mGeoAdvs)

}
