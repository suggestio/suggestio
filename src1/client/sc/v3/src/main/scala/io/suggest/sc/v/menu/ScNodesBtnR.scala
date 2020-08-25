package io.suggest.sc.v.menu

import com.materialui.{MuiListItem, MuiListItemProps, MuiListItemText}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.{MScRoot, ScNodesShowHide}
import io.suggest.sc.v.styl.{ScCss, ScCssStatic}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.2020 16:20
  * Description: Пункт меню для доступа к форме управления узлами.
  */
class ScNodesBtnR(
              scCssP        : React.Context[ScCss],
              crCtxProv     : React.Context[MCommonReactCtx],
            ) {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]

  case class State(
                    isVisibleSomeC            : ReactConnectProxy[Some[Boolean]],
                  )

  class Backend($: BackendScope[Props, State]) {

    private lazy val _onMenuItemClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, ScNodesShowHide( visible = true ) )
    }

    def render(s: State): VdomElement = {
      s.isVisibleSomeC { isVisibleSomeProxy =>
        import ScCssStatic.Menu.{Rows => R}

        val isVisible = isVisibleSomeProxy.value.value
        ReactCommonUtil.maybeEl( isVisible ) {
          MuiListItem(
            new MuiListItemProps {
              override val disableGutters = true
              override val button = true
              override val onClick = _onMenuItemClick
            }
          )(
            MuiListItemText()(
              scCssP.consume { scCss =>
                <.span(
                  R.rowContent,
                  scCss.fgColor,
                  crCtxProv.message( MsgCodes.`Nodes.management` ),
                )
              },

              // TODO Вывести счётких текущих видимых маячков.
            )
          )
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        isVisibleSomeC = propsProxy.connect { mroot =>
          OptionUtil.SomeBool( mroot.index.isLoggedIn )
        },

      )
    }
    .renderBackend[Backend]
    .build

}
