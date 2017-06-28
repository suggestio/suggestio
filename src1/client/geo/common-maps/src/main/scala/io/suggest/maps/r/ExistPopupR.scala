package io.suggest.maps.r

import diode.react.ModelProxy
import io.suggest.adv.geo.{InGeoTag, OnAdvsMap, OnGeoCapturing, OnMainScreen}
import io.suggest.common.html.HtmlConstants._
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.maps.m.MExistGeoPopupS
import io.suggest.maps.u.MapsUtil
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.react.ReactCommonUtil.Implicits.vdomElOptionExt
import io.suggest.react.r.RangeYmdR
import io.suggest.sjs.common.i18n.Messages
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import react.leaflet.popup.{LPopupPropsR, LPopupR}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.12.16 16:13
  * Description: React-компонент содержимого попапа над георазмещением на карте.
  */
object ExistPopupR {

  type Props = ModelProxy[MExistGeoPopupS]


  /** Тег с префиксом тега. */
  private lazy val _tagPrefix = {
    <.span(
      ^.`class` := Css.Colors.LIGHT_GRAY,
      ^.title   := Messages( MsgCodes.`GeoTag` ),
      TAG_PREFIX
    )
  }

  /** Тег с постфиком-галочкой. */
  private lazy val _onlineNow = {
    <.span(
      ^.title := Messages( MsgCodes.`_adv.Online.now` ),
      CHECKMARK
    )
  }


  /** Рендерер содержимого попапа. */
  class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): VdomElement = {
      val p0 = p()

      // Костыль для whenDefinedEl()
      val popDataOpt = for {
        popResp   <- p0.content.toOption
        popState  <- p0.state
      } yield {
        (popResp, popState)
      }

      popDataOpt.whenDefinedEl { case (popResp, popState) =>
        LPopupR(
          new LPopupPropsR {
            override val position = MapsUtil.geoPoint2LatLng( popState.geoPoint )
          }
        ) {
          val elements = for (row <- popResp.rows) yield {
            <.li(
              ^.key := row.dateRange.toString,

              // Рендер диапазона дат, если указан...
              RangeYmdR(
                RangeYmdR.Props(
                  capFirst = true,
                  rangeYmdOpt = row.dateRange
                )
              )
                .when(row.dateRange.nonEmpty),

              SPACE,

              // Рендер строкой всех item'ов в текущем диапазоне.
              row.items.toVdomArray { itm =>
                <.span(
                  ^.key := itm.itemId,
                  itm.payload match {
                    // lk-adv-geo
                    case OnMainScreen =>
                      Messages(MsgCodes.`Main.screen`)
                    case InGeoTag(face) =>
                      <.span(
                        _tagPrefix,
                        face
                      )
                    // lk-adn-map
                    case OnAdvsMap =>
                      Messages(MItemTypes.AdnNodeMap.nameI18n)
                    case OnGeoCapturing =>
                      Messages(MItemTypes.GeoLocCaptureArea.nameI18n)
                  },
                  _onlineNow.when(itm.isOnlineNow),
                  COMMA, SPACE
                )
              }

            )
          }
          <.ul(
            elements: _*
          )
        }: VdomElement
      }

    }

  }


  val component = ScalaComponent.builder[Props]("GeoAdvExistPop")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(mGeoAdvs: Props) = component(mGeoAdvs)

}
