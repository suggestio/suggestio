package io.suggest.bill.cart.v.pay

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.bill.cart.m.MCartRootS
import io.suggest.bill.cart.v.pay.systems.YooKassaPayWidgetR
import io.suggest.lk.r.FlexCenteredR
import io.suggest.pay.{MPaySystem, MPaySystems}
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.Implicits._
import io.suggest.react.r.CatchR
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/** Top-level component over all pay-subsystem subcomponents. */
class CartPayR(
                paySystemScriptR    : PaySystemScriptR,
                yooKassaPayWidgetR  : => YooKassaPayWidgetR,
              ) {

  type Props_t = MCartRootS
  type Props = ModelProxy[Props_t]

  case class State(
                    paySystemForWidgetPotC      : ReactConnectProxy[Pot[MPaySystem]],
                    prefmtFooterOptC            : ReactConnectProxy[Option[String]],
                  )

  class Backend( $: BackendScope[Props, _] ) {

    def render(p: Props, s: State): VdomElement = {
      React.Fragment(
        <.div(

          // Render widget component, when everything else is ready:
          s.paySystemForWidgetPotC { paySystemPotProxy =>
            paySystemPotProxy
              .value
              .toOption
              .whenDefinedEl { paySystem =>
                CatchR.component( paySystemPotProxy.resetZoom(paySystem.value) )(
                  paySystem match {
                    case MPaySystems.YooKassa =>
                      yooKassaPayWidgetR.component( p )
                    case _ =>
                      ReactCommonUtil.VdomNullElement
                  }
                )
              }
          },

          // external payment system widget script tag:
          p.wrap(_.pay.paySystemInit)( paySystemScriptR.component.apply ),

        ),

        // Render optional preformatted footer, if any:
        s.prefmtFooterOptC { prefmtFooterProxy =>
          prefmtFooterProxy.value.whenDefinedEl { prefmtFooter =>
            FlexCenteredR.component(true)(
              <.pre(
                prefmtFooter,
              ),
            )
          }
        },

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

        prefmtFooterOptC = propsProxy.connect { mroot =>
          mroot.pay.cartSubmit
            .toOption
            .flatMap( _.pay )
            .flatMap( _.prefmtFooter )
        },

      )
    }
    .renderBackend[Backend]
    .build

}
