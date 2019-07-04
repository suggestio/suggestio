package io.suggest.id.login.v.stuff

import com.materialui.{MuiCheckBox, MuiCheckBoxClasses, MuiCheckBoxProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.id.login.m.ICheckBoxActionStatic
import io.suggest.id.login.v.LoginFormCss
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, React, ReactEventFromInput, ScalaComponent}
import japgolly.univeq._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.19 18:03
  * Description: wrap() react-компонент галочки для формы логина/регистрации.
  */
class CheckBoxR(
                 loginFormCssCtx     : React.Context[LoginFormCss],
               ) {

  case class PropsVal(
                       checked    : Boolean,
                       onChange   : ICheckBoxActionStatic,
                     )
  implicit object CheckBoxRFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.checked ==* b.checked) &&
      (a.onChange ===* b.onChange)
    }
  }

  type Props_t  = PropsVal
  type Props    = ModelProxy[Props_t]


  case class State(
                    checkedSomeC    : ReactConnectProxy[Some[Boolean]],
                  )

  class Backend($: BackendScope[Props, State]) {

    private def _onForeignPcChange( event: ReactEventFromInput, checked: Boolean ): Callback = {
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { propsProxy: Props =>
        propsProxy.value.onChange( checked )
      }
    }
    private val _onForeignPcChangeCbF = ReactCommonUtil.cbFun2ToJsCb( _onForeignPcChange )


    def render(p: Props, s: State): VdomElement = {
      loginFormCssCtx.consume { loginFormCss =>
        s.checkedSomeC { checkedSomeProxy =>
          val cbCss = new MuiCheckBoxClasses {
            override val root = loginFormCss.formControl.htmlClass
          }
          MuiCheckBox(
            new MuiCheckBoxProps {
              @JSName("onChange")
              override val onChange2 = _onForeignPcChangeCbF
              override val checked = js.defined( checkedSomeProxy.value.value )
              override val classes = cbCss
            }
          )
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        checkedSomeC = propsProxy.connect { p =>
          OptionUtil.SomeBool( p.checked )
        }( FastEq.AnyRefEq ),
      )
    }
    .renderBackend[Backend]
    .build

  def apply(propsProxy: Props) = component( propsProxy )

}
