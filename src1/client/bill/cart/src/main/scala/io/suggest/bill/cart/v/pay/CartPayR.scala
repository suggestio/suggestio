package io.suggest.bill.cart.v.pay

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.bill.cart.m.MCartRootS
import io.suggest.bill.cart.v.pay.systems.YooKassaCartR
import io.suggest.common.empty.OptionUtil
import io.suggest.mbill2.m.order.MOrderStatuses
import io.suggest.pay.{MPaySystem, MPaySystems}
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.Implicits._
import io.suggest.react.r.CatchR
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/** Top-level component over all pay-subsystem subcomponents. */
class CartPayR(
                paySystemScriptR    : PaySystemScriptR,
                yooKassaCartR       : => YooKassaCartR,
                val goToPayBtnR     : PayButtonR,
              ) {

  type Props_t = MCartRootS
  type Props = ModelProxy[Props_t]

  case class State(
                    paySystemForWidgetPotC      : ReactConnectProxy[Pot[MPaySystem]],
                    payButtonEnabledOptC        : ReactConnectProxy[goToPayBtnR.Props_t],
                  )

  class Backend( $: BackendScope[Props, _] ) {

    def render(p: Props, s: State): VdomElement = {
      <.div(

        // PAY button, when payment is possible (non-empty cart-order).
        s.payButtonEnabledOptC { goToPayBtnR.component.apply },

        // Render widget component, when everything else is ready:
        s.paySystemForWidgetPotC { paySystemPotProxy =>
          paySystemPotProxy
            .value
            .toOption
            .whenDefinedEl { paySystem =>
              CatchR.component( paySystemPotProxy.resetZoom(paySystem.value) )(
                paySystem match {
                  case MPaySystems.YooKassa =>
                    yooKassaCartR.component( p )
                  case _ =>
                    ReactCommonUtil.VdomNullElement
                }
              )
            }
        },

        // script tag:
        p.wrap(_.pay.paySystemInit)( paySystemScriptR.component.apply ),

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]
    .initialStateFromProps { propsProxy =>
      State(

        paySystemForWidgetPotC = propsProxy.connect { mroot =>
          val props = mroot.pay.paySystemInit
          if (props.isPending || props.isFailed)
            Pot.empty
          else
            props
        },

        payButtonEnabledOptC = propsProxy.connect { props =>
          OptionUtil.maybeOpt(
            props.order.orderContents.exists { ocJs =>
              val oc = ocJs.content
              oc.items.nonEmpty &&
                oc.order.exists(_.status ==* MOrderStatuses.Draft)
            } && {
              val psIni = props.pay.paySystemInit
              psIni.isEmpty || psIni.isFailed
            }
          ) (
            OptionUtil.SomeBool {
              !props.order.orderContents.isPending &&
              !props.pay.cartSubmit.isPending &&
              !props.pay.paySystemInit.isPending
            }
          )
        },

      )
    }
    .renderBackend[Backend]
    .build

}
