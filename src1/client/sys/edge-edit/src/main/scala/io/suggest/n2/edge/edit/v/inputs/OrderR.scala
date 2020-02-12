package io.suggest.n2.edge.edit.v.inputs

import com.materialui.{MuiFormControlClasses, MuiTextField, MuiTextFieldProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.n2.edge.edit.m.OrderSet
import io.suggest.n2.edge.edit.v.EdgeEditCss
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.2020 11:18
  * Description: Компонент выставление порядкового номера эджа.
  */
class OrderR(
              crCtxProv            : React.Context[MCommonReactCtx],
            ) {

  type Props_t = Option[Int]
  type Props = ModelProxy[Props_t]


  case class State(
                    orderOptC       : ReactConnectProxy[Props_t],
                  )


  class Backend( $: BackendScope[Props, State] ) {

    private val _onChangeCbF = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val orderOpt = Try( e.target.value.toInt )
        .toOption
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, OrderSet(orderOpt) )
    }

    def render(s: State): VdomElement = {
      val _label = crCtxProv.message( MsgCodes.`Ordering` )

      val css = new MuiFormControlClasses {
        override val root = EdgeEditCss.inputLeft.htmlClass
      }

      s.orderOptC { orderOptProxy =>
        val _value = orderOptProxy
          .value
          .fold("")(_.toString)

        MuiTextField(
          new MuiTextFieldProps {
            override val label  = _label.rawNode
            override val `type` = HtmlConstants.Input.number
            override val value  = _value
            override val onChange = _onChangeCbF
            override val classes  = css
          }
        )()
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        orderOptC = propsProxy.connect( identity ),
      )
    }
    .renderBackend[Backend]
    .build

}
