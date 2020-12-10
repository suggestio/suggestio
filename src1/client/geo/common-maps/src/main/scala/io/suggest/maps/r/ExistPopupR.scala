package io.suggest.maps.r

import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.common.html.HtmlConstants._
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.maps.m.MExistGeoPopupS
import io.suggest.maps.u.MapsUtil
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.r.RangeYmdR
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

  type Props_t = MExistGeoPopupS
  type Props = ModelProxy[Props_t]


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
                  itm.itemType match {
                    // lk-adv-geo
                    case MItemTypes.GeoPlace =>
                      Messages(MsgCodes.`Main.screen`)
                    case MItemTypes.GeoTag =>
                      <.span(
                        _tagPrefix,
                        itm.tagFace.whenDefined,
                      )
                    // lk-adn-map
                    case MItemTypes.GeoLocCaptureArea =>
                      Messages(MItemTypes.GeoLocCaptureArea.nameI18n)
                    case other =>
                      "TODO: " + other
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


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  private def _apply(mGeoAdvs: Props) = component(mGeoAdvs)
  val apply: ReactConnectProps[Props_t] = _apply

}
