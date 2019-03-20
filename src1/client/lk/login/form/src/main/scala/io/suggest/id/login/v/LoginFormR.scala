package io.suggest.id.login.v

import chandu0101.scalajs.react.components.materialui.{MuiDialog, MuiDialogContent, MuiDialogMaxWidths, MuiDialogProps, MuiDialogTitle, MuiPaper, MuiPaperProps, MuiTab, MuiTabProps, MuiTabs, MuiTabsProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.css.CssR
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.login.m._
import io.suggest.id.login.m.epw.MEpwLoginS
import io.suggest.id.login.m.ext.MExtLoginFormS
import io.suggest.id.login.v.epw.EpwFormR
import io.suggest.id.login.v.ext.ExtFormR
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react.{BackendScope, Callback, React, ReactEvent, ReactEventFromHtml, ScalaComponent}
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
                  epwFormR              : EpwFormR,
                  extFormR              : ExtFormR,
                  commonReactCtxProv    : React.Context[MCommonReactCtx],
                  loginFormCssCtx       : React.Context[LoginFormCss],
                ) {

  type Props_t = MLoginRootS
  type Props = ModelProxy[Props_t]


  case class State(
                    visibleSomeC          : ReactConnectProxy[Some[Boolean]],
                    currTabC              : ReactConnectProxy[MLoginTab],
                    loginFormCssC         : ReactConnectProxy[LoginFormCss],
                  )


  private def _tabBtn( tab: MLoginTab ): VdomElement = {
    MuiTab {
      // Получить messages через контекст:
      val labelText = commonReactCtxProv.consume { crCtx =>
        crCtx.messages( tab.msgCode )
      }
      new MuiTabProps {
        override val value = js.defined( tab.value )
        override val label = js.defined {
          labelText.rawNode
        }
      }
    }
  }


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
      // Форма логина через внешние сервисы.
      val extLogin = p.wrap(_.ext)( extFormR.apply )( implicitly, MExtLoginFormS.MExtLoginFormSFastEq )

      // Содержимое вкладки входа по логину и паролю:
      lazy val epwLogin = p.wrap(_.epw)( epwFormR.apply )( implicitly, MEpwLoginS.MEpwLoginSFastEq )

      // Содержимое табов:
      val tabsContents = <.div(
        s.currTabC { currTabProxy =>
          // TODO Запилить react-swipeable-views, как в примерах MuiTabs.
          currTabProxy.value match {
            case MLoginTabs.Ext => extLogin
            case MLoginTabs.Epw => epwLogin
          }
        }
      )

      // кнопка таба EmailPw-логина:
      val epwTabBtn =  _tabBtn( MLoginTabs.Epw )
      val extTabBtn = _tabBtn( MLoginTabs.Ext )

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
            extTabBtn,
            epwTabBtn,
          )
        }
      )

      // Заголовок диалога
      val dialogTitle = MuiDialogTitle()(
        commonReactCtxProv.consume { crCtx =>
          crCtx.messages( MsgCodes.`Login.page.title` )
        }
      )

      // Наполнение диалога.
      val dialogContent = MuiDialogContent()(
        tabsContents,
        tabs,
      )

      // Весь диалог формы логина:
      val allForm = s.visibleSomeC { visibleSomeProxy =>
        MuiDialog(
          new MuiDialogProps {
            override val maxWidth = js.defined {
              MuiDialogMaxWidths.xs
            }
            override val open = visibleSomeProxy.value.value
            override val onClose = _onLoginCloseCbF
            // TODO disable* -- true на одинокой форме. false/undefined для формы, встраиваемой в выдачу.
            override val disableBackdropClick = true
            override val disableEscapeKeyDown = true
          }
        )(
          dialogTitle,
          dialogContent
        )
      }

      // Добавить внутренний контекст для CSS.
      s.loginFormCssC { loginFormCssProxy =>
        <.div(
          CssR( loginFormCssProxy ),

          loginFormCssCtx.provide( loginFormCssProxy.value )(
            allForm
          )
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { rootProxy =>
      State(
        visibleSomeC        = rootProxy.connect( _.overall.isVisibleSome )( FastEqUtil.RefValFastEq ),
        currTabC            = rootProxy.connect( _.overall.loginTab )( FastEq.AnyRefEq ),
        loginFormCssC       = rootProxy.connect( _.overall.formCss )( FastEq.AnyRefEq ),
      )
    }
    .renderBackend[Backend]
    .build

  def apply(propsProxy: Props) = component( propsProxy )

}
