package io.suggest.id.login.v.ext

import chandu0101.scalajs.react.components.materialui.{MuiButtonBase, MuiButtonBaseProps, MuiPaper}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.css.Css
import io.suggest.ext.svc.{MExtService, MExtServices}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.login.m.{ExtLoginVia, LoginProgressR}
import io.suggest.id.login.m.ext.MExtLoginFormS
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.FastEqUtil
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
                commonReactCtxProv          : React.Context[MCommonReactCtx],
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
            commonReactCtxProv.consume { crCtx =>
              crCtx.messages( MsgCodes.`ESIA._unabbrevated` )
            },
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
              loginProgressR( loginUrlReqPendingSomeProxy ),
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
        loginUrlReqPendingSomeC = propsProxy.connect( _.loginUrlReqPendingSome )( FastEqUtil.RefValFastEq ),
      )
    }
    .renderBackend[Backend]
    .build

  def apply(extFormProxy: Props) = component( extFormProxy )

}
