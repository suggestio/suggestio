package io.suggest.id.login.v.ext

import com.materialui.{MuiButtonBase, MuiButtonBaseProps, MuiPaper}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.css.Css
import io.suggest.ext.svc.{MExtService, MExtServices}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.login.m.ExtLoginVia
import io.suggest.id.login.m.ext.MExtLoginFormS
import io.suggest.id.login.v.stuff.LoginProgressR
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react.{BackendScope, Callback, React, ReactEvent, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.03.19 17:21
  * Description: Форма логина через внешний сервис.
  * Используется через ReactConnector.wrap().
  */
class ExtFormR(
                loginProgressR              : LoginProgressR,
                crCtxProv                   : React.Context[MCommonReactCtx],
              ) {

  type Props_t = MExtLoginFormS
  type Props = ModelProxy[Props_t]


  case class State(
                    loginUrlReqPendingSomeC      : ReactConnectProxy[Some[Boolean]],
                  )


  class Backend($: BackendScope[Props, State]) {

    private def _onLoginClick(extService: MExtService)(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, ExtLoginVia(extService) )
    private val _onLoginClickEsiaCbF = ReactCommonUtil.cbFun1ToJsCb( _onLoginClick( MExtServices.GosUslugi ) )

    def render(s: State): VdomElement = {
      MuiPaper()(

        // Кнопка на логин через ЕСИА:
        {
          val btnIcon = <.div(
            ^.`class` := Css.GosUslugi.LOGO,
          )

          val btnTitle = <.div(
            ^.`class` := Css.GosUslugi.ESIA_TITLE,
            crCtxProv.message( MsgCodes.`ESIA._unabbrevated` ),
          )

          s.loginUrlReqPendingSomeC { loginUrlReqPendingSomeProxy =>
            val isDisabled = loginUrlReqPendingSomeProxy.value.value
            <.span(
              // Кнопка для логина
              MuiButtonBase(
                new MuiButtonBaseProps {
                  override val focusRipple  = !isDisabled
                  override val onClick      = _onLoginClickEsiaCbF
                  override val disabled     = isDisabled
                  override val disableRipple = true
                }
              )(
                btnIcon,
                btnTitle,
              ),

              // Прогрессбар ожидания:
              loginProgressR.component( loginUrlReqPendingSomeProxy ),
            )
          }
        },

      )
    }
  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        loginUrlReqPendingSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.loginUrlReq.isPending )
        }( FastEq.AnyRefEq ),
      )
    }
    .renderBackend[Backend]
    .build

}
