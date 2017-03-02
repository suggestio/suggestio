package io.suggest.lk.adv.r

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import diode.react.ReactPot._
import io.suggest.bill.{MGetPriceResp, MPrice}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css.Lk.Bars.RightBar.Price
import io.suggest.css.Css
import io.suggest.lk.adv.m.{DoFormSubmit, MPriceS}
import io.suggest.sjs.common.i18n.Messages
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement}
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

    /** Нажатие кнопки сабмита всея формы. */
    def onSubmitBtnClick: Callback = {
      $.props >>= { p =>
        p.dispatchCB( DoFormSubmit )
      }
    }

    def render(s: State): ReactElement = {
      <.div(
        ^.`class` := Price.WIDGET_CNT,

        <.h2(
          ^.`class` := Price.WIDGET_TITLE,
          Messages("Total.amount._money")
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
                for (p <- resp.prices) yield {
                  Messages(p.currency.i18nPriceCode, MPrice.amountStr(p))
                }
              }
            ),

            // Отрендерить loader, если происходит пересчёт ценника.
            pricePot.renderPending { _ =>
              <.div(
                ^.`class` := Price.WIDGET_LOADER,
                ^.title   := Messages("Please.wait")
              )
            },

            // Рендер ошибки тут же. TODO Может есть более лучшее место для этого?
            pricePot.renderFailed { ex =>
              <.p(
                ^.`class` := Css.Colors.RED + HtmlConstants.SPACE + Css.Text.CENTERED,
                ^.title := ex.getClass.getSimpleName + HtmlConstants.SPACE + ex.getMessage,

                Messages("Error"), HtmlConstants.ELLIPSIS
              )
            }
          )
        },

        <.button(
          ^.`class` := (Price.WIDGET_REQ_BTN :: Css.Buttons.BTN :: Css.Buttons.MAJOR :: Css.Size.L :: Nil)
            .mkString(HtmlConstants.SPACE),
          ^.onClick --> onSubmitBtnClick,

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
