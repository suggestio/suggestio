package io.suggest.lk.adv.r

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import diode.react.ReactPot._
import io.suggest.bill.MGetPriceResp
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css.Lk.Bars.RightBar.Price
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.adv.m.{DoFormSubmit, MPriceS}
import io.suggest.sjs.common.i18n.{JsFormatUtil, Messages}
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.01.17 11:09
  * Description: React-компонент для виджета цены с кнопкой подтверждения размещения.
  */
object PriceR {

  type Props = ModelProxy[MPriceS]

  protected case class State(
                              pricePotConn: ReactConnectProxy[Pot[MGetPriceResp]]
                            )

  protected class Backend($: BackendScope[Props, State]) {

    /** Нажатие кнопки сабмита всея формы. */
    def onSubmitBtnClick: Callback = {
      dispatchOnProxyScopeCB($, DoFormSubmit)
    }

    def render(s: State): VdomElement = {
      <.div(
        ^.`class` := Price.WIDGET_CNT,

        <.h2(
          ^.`class` := Price.WIDGET_TITLE,
          Messages( MsgCodes.`Total.amount._money` )
        ),

        s.pricePotConn { pricePotProx =>
          val pricePot = pricePotProx()

          <.span(
            <.p(
              ^.`class` := Price.WIDGET_PRICE_VALUE,

              pricePot.renderEmpty {
                HtmlConstants.ELLIPSIS
              },

              pricePot.render { resp =>
                resp.prices.toVdomArray { p =>
                  JsFormatUtil.formatPrice(p)
                }
              }
            ),

            // Отрендерить loader, если происходит пересчёт ценника.
            pricePot.renderPending { _ =>
              <.div(
                ^.`class` := Price.WIDGET_LOADER,
                ^.title   := Messages( MsgCodes.`Please.wait` )
              )
            },

            // Рендер ошибки тут же. TODO Может есть более лучшее место для этого?
            pricePot.renderFailed { ex =>
              <.p(
                ^.`class` := Css.Colors.RED + HtmlConstants.SPACE + Css.Text.CENTERED,
                ^.title := ex.getClass.getSimpleName + HtmlConstants.SPACE + ex.getMessage,

                Messages(  MsgCodes.`Error` ), HtmlConstants.ELLIPSIS
              )
            }
          )
        },

        <.button(
          ^.`class` := Css.flat(Price.WIDGET_REQ_BTN, Css.Buttons.BTN, Css.Buttons.MAJOR, Css.Size.L),
          ^.onClick --> onSubmitBtnClick,

          Messages( MsgCodes.`Send.request` )
        )

      )
    }
  }

  val component = ScalaComponent.builder[Props]("PriceWdgt")
    .initialStateFromProps { p =>
      State(
        pricePotConn = p.connect(_.resp)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
