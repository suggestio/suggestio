package io.suggest.id.login.v

import chandu0101.scalajs.react.components.materialui.{MuiDialog, MuiDialogProps, MuiPaper, MuiPaperProps, MuiTab, MuiTabProps, MuiTabs, MuiTabsProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.i18n.MsgCodes
import io.suggest.id.login.m._
import io.suggest.msg.Messages
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react.{BackendScope, Callback, ReactEvent, ReactEventFromHtml, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 15:31
  * Description: Корневой компонент формы логина.
  */
class LoginFormR(
                  epwFormR          : EpwFormR,
                ) {

  type Props_t = MLoginRootS
  type Props = ModelProxy[Props_t]

  case class State(
                    visibleSomeC          : ReactConnectProxy[Some[Boolean]],
                    currTabC              : ReactConnectProxy[MLoginTab],
                  )

  class Backend($: BackendScope[Props, State]) {

    private def _onTabChanged(event: ReactEventFromHtml, newValue: js.Any): Callback = {
      val newTab = MLoginTabs.withValue( newValue.asInstanceOf[Int] )
      ReactDiodeUtil.dispatchOnProxyScopeCB($, SwitсhLoginTab(newTab) )
    }
    private val _onTabChangedCbF = ReactCommonUtil.cbFun2ToJsCb( _onTabChanged )


    private def _onLoginClose(event: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB($, LoginShowHide(false))
    private val _onLoginCloseCbF = ReactCommonUtil.cbFun1ToJsCb( _onLoginClose )


    def render(p: Props, s: State): VdomElement = {
      // Содержимое таба с логином и паролем:
      val epw = p.wrap(_.epw)(epwFormR.apply)( implicitly, MEpwLoginS.MEpwLoginSFastEq )

      // Содержимое табов:
      val tabsContents = s.currTabC { currTabProxy =>
        <.div(
          // TODO Запилить react-swipeable-views, как в примерах MuiTabs.
          currTabProxy.value match {
            case MLoginTabs.Epw => epw
          }
        )
      }

      // кнопка таба EmailPw-логина:
      val epwTabBtn = MuiTab(
        new MuiTabProps {
          override val value = js.defined( MLoginTabs.Epw.value )
          override val label = js.defined {
            Messages( MsgCodes.`Login.using.password` )
              .rawNode
          }
        }
      )

      // Список табов:
      val tabs = MuiPaper(
        new MuiPaperProps {
          override val square = true
        }
      )(
        s.currTabC { currTabProxy =>
          MuiTabs(
            new MuiTabsProps {
              override val value = js.defined( currTabProxy.value.value )
              @JSName("onChange")
              override val onTabChanged = _onTabChangedCbF
            }
          )(
            epwTabBtn,
          )
        }
      )

      // Весь диалог формы логина:
      s.visibleSomeC { visibleSomeProxy =>
        MuiDialog(
          new MuiDialogProps {
            override val open = visibleSomeProxy.value.value
            override val onClose = _onLoginCloseCbF
          }
        )(
          tabsContents,
          tabs,
        )
      }
    }

  }

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { rootProxy =>
      State(
        visibleSomeC = rootProxy.connect( _.overall.isVisibleSome )( FastEqUtil.RefValFastEq ),
        currTabC     = rootProxy.connect( _.overall.loginTab )( FastEq.AnyRefEq ),
      )
    }
    .renderBackend[Backend]
    .build

  def apply(propsProxy: Props) = component( propsProxy )

}
