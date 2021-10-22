package io.suggest.bill.cart.v.pay

import com.materialui.{Mui, MuiBox, MuiColorTypes, MuiFab, MuiFabClasses, MuiFabProps, MuiFabVariants, MuiSx}
import diode.react.ModelProxy
import io.suggest.bill.cart.m.CartSubmit
import io.suggest.bill.cart.v.order.OrderCss
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
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

  type Props_t = Option[Boolean]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    private val _onPayButtonClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, CartSubmit() )
    }

    def render(propsProxy: Props): VdomElement = {
      propsProxy.value.whenDefinedEl { isEnabled =>
        // For centering inside flex toolbar, use this as left/right placeholders:
        val placeHolder = MuiBox.component(
          new MuiBox.Props {
            override val sx = new MuiSx {
              override val flexGrow = 1: js.Any
            }
          }
        )()

        React.Fragment(

          placeHolder,

          MuiFab {
            new MuiFabProps {
              override val variant    = MuiFabVariants.extended
              override val color      = MuiColorTypes.secondary
              override val disabled   = !isEnabled
              override val onClick    = _onPayButtonClick
              override val classes    = new MuiFabClasses {
                override val root = orderCss.PayBtn.root.htmlClass
              }
            }
          } (
            Mui.SvgIcons.Payment()(),
            HtmlConstants.NBSP_STR,
            crCtxP.message( MsgCodes.`Pay` ),
          ),

          placeHolder,

        )
      }
    }

  }

  val component = ScalaComponent
    .builder[Props]
    .stateless
    .renderBackend[Backend]
    .build

}
