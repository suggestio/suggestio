package io.suggest.id.login.v.stuff

import chandu0101.scalajs.react.components.materialui.{MuiButton, MuiButtonClasses, MuiButtonProps, MuiButtonSizes, MuiButtonVariants}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.i18n.MCommonReactCtx
import io.suggest.id.login.m.ILoginFormAction
import io.suggest.id.login.v.LoginFormCss
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react.{BackendScope, Callback, React, ReactEvent, ScalaComponent}
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.03.19 14:11
  * Description: wrap-компонент для кнопки формы логина.
  */
class ButtonR(
               commonReactCtxProv          : React.Context[MCommonReactCtx],
               loginFormCssCtx             : React.Context[LoginFormCss],
             ) {

  case class PropsVal(
                       disabled   : Boolean,
                       onClick    : ILoginFormAction,
                       msgCode    : String,
                     )
  implicit object ButtonRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.disabled ==* b.disabled) &&
      (a.onClick ===* b.onClick) &&
      (a.msgCode ===* b.msgCode)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  case class State(
                    disabledSomeC : ReactConnectProxy[Some[Boolean]],
                    msgCodeS      : ReactConnectProxy[String],
                  )

  class Backend($: BackendScope[Props, State]) {

    private def _onBtnClick(event: ReactEvent): Callback = {
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { propsProxy: Props =>
        propsProxy.value.onClick
      }
    }
    private val _onBtnClickCbF = ReactCommonUtil.cbFun1ToJsCb( _onBtnClick )


    def render(propsProxy: Props, s: State): VdomElement = {
      val loginText = s.msgCodeS { msgCodeProxy =>
        commonReactCtxProv.consume { crCtx =>
          crCtx.messages( msgCodeProxy.value )
        }
      }

      loginFormCssCtx.consume { loginFormCss =>
        val btnCss = new MuiButtonClasses {
          override val root = loginFormCss.formControl.htmlClass
        }
        s.disabledSomeC { disabledSomeProxy =>
          MuiButton(
            new MuiButtonProps {
              override val size       = MuiButtonSizes.large
              override val variant    = MuiButtonVariants.contained
              override val onClick    = _onBtnClickCbF
              override val disabled   = disabledSomeProxy.value.value
              override val fullWidth  = true
              override val classes    = btnCss
            }
          )(
            loginText
          )
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        disabledSomeC = propsProxy.connect { props =>
          Some( props.disabled )
        }( FastEq.ValueEq ),
        msgCodeS = propsProxy.connect(_.msgCode)( FastEq.AnyRefEq ),
      )
    }
    .renderBackend[Backend]
    .build

  def apply( propsProxy: Props ) = component( propsProxy )

}
