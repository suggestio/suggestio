package io.suggest.lk.adv.r

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import diode.react.ReactPot._
import io.suggest.bill.{MGetPriceResp, MPrice}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css.Lk.Bars.RightBar.Price
import io.suggest.css.Css
import io.suggest.lk.adv.m.MPriceS
import io.suggest.lk.vm.LkMessagesWindow.Messages
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import japgolly.scalajs.react.vdom.prefix_<^._

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

    def render(s: State): ReactElement = {
      <.div(
        ^.`class` := Price.WIDGET_CNT,

        <.h2(
          ^.`class` := Price.WIDGET_TITLE,
          Messages("Total.amount._money")
        ),

        s.pricePotConn { pricePotProx =>
          val pricePot = pricePotProx()

          <.p(
            ^.`class` := Price.WIDGET_PRICE_VALUE,

            pricePot.renderEmpty {
              HtmlConstants.ELLIPSIS
            },

            pricePot.renderReady { resp =>
              for (p <- resp.prices) yield {
                Messages(p.currency.i18nPriceCode, MPrice.amountStr(p))
              }
            }

            // TODO renderPending: рендерить loader-крутилку поверх блеклой цены, например.
          )
        },

        <.button(
          ^.`class` := (Price.WIDGET_REQ_BTN :: Css.Buttons.BTN :: Css.Buttons.MAJOR :: Css.Size.L :: Nil)
            .mkString(HtmlConstants.SPACE),
          Messages("Send.request")
        )
      )
    }
  }

  val component = ReactComponentB[Props]("PriceWdgt")
    .initialState_P { p =>
      State(
        pricePotConn = p.connect(_.resp)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
