package io.suggest.bill.cart.v.pay

import com.materialui.{Mui, MuiBox, MuiColorTypes, MuiFab, MuiFabClasses, MuiFabProps, MuiFabVariants, MuiSx}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.bill.cart.MPayableVia
import io.suggest.bill.cart.m.{CartSubmit, MCartRootS}
import io.suggest.bill.cart.v.order.OrderCss
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants._
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.r.FlexCenteredR
import io.suggest.mbill2.m.order.MOrderStatuses
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.09.18 17:58
  * Description: Go to pay form button component.
  */
class PayButtonR(
                  orderCss    : OrderCss,
                  crCtxP      : React.Context[MCommonReactCtx],
                ) {

  type Props_t = MCartRootS
  type Props = ModelProxy[Props_t]


  case class State(
                    isVisibleEnabledOptC    : ReactConnectProxy[Option[Boolean]],
                    payableViasC            : ReactConnectProxy[Seq[MPayableVia]],
                  )


  class Backend($: BackendScope[Props, State]) {

    private def _onPayButtonClick(payVia: MPayableVia) = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, CartSubmit(payVia) )
    }

    def render(s: State): VdomElement = {
      s.isVisibleEnabledOptC { isVisibleEnabledOptProxy =>
        isVisibleEnabledOptProxy.value.whenDefinedEl { isEnabled =>
          val payMsg = crCtxP.message( MsgCodes.`Pay` )
          val payBtnCss = new MuiFabClasses {
            override val root = orderCss.PayBtn.root.htmlClass
          }

          FlexCenteredR.component(false)(
            s.payableViasC { payableViasProxy =>
              val payableVias = payableViasProxy.value

              // TODO Render info, if payableVias is empty, but order have items?
              React.Fragment(
                payableVias.toVdomArray { payableVia =>
                  MuiFab.component.withKey( payableVia.toString ) {
                    new MuiFabProps {
                      override val variant    = MuiFabVariants.extended
                      override val color      = payableVia.isTest match {
                        case false => MuiColorTypes.secondary
                        case true  => MuiColorTypes.disabled
                      }
                      override val disabled   = !isEnabled
                      override val onClick    = _onPayButtonClick( payableVia )
                      override val classes    = payBtnCss
                    }
                  } (
                    (payableVia.isTest match {
                      case true   => Mui.SvgIcons.MoneyOff
                      case false  => Mui.SvgIcons.Payment
                    })()(),
                    NBSP_STR,
                    payMsg,
                    ReactCommonUtil.maybeNode( payableVia.isTest ) (React.Fragment(
                      NBSP_STR, `(`, MsgCodes.`Test`, `)`,
                    )),
                  )
                },
              )
            },
          )
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]
    .initialStateFromProps { propsProxy =>
      State(

        isVisibleEnabledOptC = propsProxy.connect { props =>
          OptionUtil.maybeOpt(
            props.order.orderContents.exists { ocJs =>
              val oc = ocJs.content
              oc.items.nonEmpty &&
                oc.order.exists(_.status ==* MOrderStatuses.Draft)
            } && {
              val psIni = props.pay.paySystemInit
              psIni.isEmpty || psIni.isFailed
            } && {
              props.order.itemsSelected.isEmpty
            }
          ) (
            OptionUtil.SomeBool {
              !props.order.orderContents.isPending &&
              !props.pay.cartSubmit.isPending &&
              !props.pay.paySystemInit.isPending
            }
          )
        },

        payableViasC = propsProxy.connect { props =>
          props.order.orderContents
            .fold [Seq[MPayableVia]] (Nil) ( _.content.payableVia )
        },

      )
    }
    .renderBackend[Backend]
    .build

}
