package io.suggest.bill.cart.v.order

import com.materialui.{Mui, MuiColorTypes, MuiFab, MuiFabClasses, MuiFabProps, MuiFabVariants}
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.routes.routes
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.09.18 17:58
  * Description: Компонент для кнопки перехода к оплате.
  */
class GoToPayBtnR(
                   orderCss: OrderCss
                 ) {

  case class PropsVal(
                       onNodeId: String,
                       disabled: Boolean,
                     )
  implicit object GoToPayBtnRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.onNodeId ===* b.onNodeId) &&
        (a.disabled ==* b.disabled)
    }
  }

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    def render(propsProxy: Props): VdomElement = {
      propsProxy.value.whenDefinedEl { props =>
        val route = routes.controllers.LkBill2.cartSubmit( props.onNodeId )
        <.form(
          ^.action := route.url,
          ^.method := route.method,

          MuiFab {
            val css = new MuiFabClasses {
              override val root = orderCss.PayBtn.root.htmlClass
            }
            new MuiFabProps {
              override val classes    = css
              override val variant    = MuiFabVariants.extended
              override val `type`     = HtmlConstants.Input.submit
              override val color      = MuiColorTypes.secondary
              override val disabled   = props.disabled
            }
          }(
            Mui.SvgIcons.Payment()(),
            HtmlConstants.NBSP_STR,
            Messages( MsgCodes.`Pay` )
          )

        )
      }
    }

  }

  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsOptProxy: Props) = component( propsOptProxy )

}
