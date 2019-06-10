package io.suggest.id.login.v

import chandu0101.scalajs.react.components.materialui.{MuiDialog, MuiDialogContent, MuiDialogMaxWidths, MuiDialogProps, MuiDialogTitle, MuiPaper, MuiPaperProps, MuiTab, MuiTabProps, MuiTabs, MuiTabsProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.css.CssR
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.login.{MLoginTab, MLoginTabs}
import io.suggest.id.login.m._
import io.suggest.id.login.m.epw.MEpwLoginS
import io.suggest.id.login.m.ext.MExtLoginFormS
import io.suggest.id.login.m.reg.MRegS
import io.suggest.id.login.v.epw.EpwFormR
import io.suggest.id.login.v.ext.ExtFormR
import io.suggest.id.login.v.reg.RegR
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
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
                  epwRegR               : RegR,
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
      lazy val extLogin = p.wrap(_.ext)( extFormR.apply )( implicitly, MExtLoginFormS.MExtLoginFormSFastEq )

      // Содержимое вкладки входа по логину и паролю:
      lazy val epwLogin = p.wrap(_.epw)( epwFormR.apply )( implicitly, MEpwLoginS.MEpwLoginSFastEq )

      // Вкладка регистрации по email и паролю.
      lazy val epwReg = p.wrap(_.reg)( epwRegR.apply )( implicitly, MRegS.MEpwRegSFastEq )

      // Содержимое табов:
      val tabsContents = s.currTabC { currTabProxy =>
        // TODO Запилить react-swipeable-views, как в примерах MuiTabs.
        currTabProxy.value match {
          case MLoginTabs.EpwLogin  => epwLogin
          case MLoginTabs.EpwReg    => epwReg
          case MLoginTabs.Ext       => extLogin
        }
      }

      // кнопка таба EmailPw-логина:
      val epwLoginTabBtn  = _tabBtn( MLoginTabs.EpwLogin )
      //val extTabBtn       = _tabBtn( MLoginTabs.Ext )
      val epwRegTabBtn    = _tabBtn( MLoginTabs.EpwReg )

      // Список табов:
      val tabs = s.currTabC { currTabProxy =>
        MuiTabs(
          new MuiTabsProps {
            override val value = js.defined( currTabProxy.value.value )
            @JSName("onChange")
            override val onTabChanged = _onTabChangedCbF
          }
        )(
          //extTabBtn,
          epwLoginTabBtn,
          epwRegTabBtn,
        )
      }

      // Заголовок диалога
      val dialogTitle = MuiDialogTitle()(
        tabs,
        //commonReactCtxProv.consume { crCtx =>
          //crCtx.messages( MsgCodes.`Login.page.title` )
        //}
      )

      // Наполнение диалога.
      val dialogContent = MuiDialogContent()(
        tabsContents,
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
        visibleSomeC        = rootProxy.connect { props =>
          OptionUtil.SomeBool( props.overall.isVisible )
        }( FastEq.AnyRefEq ),
        currTabC            = rootProxy.connect( _.overall.loginTab )( FastEq.AnyRefEq ),
        loginFormCssC       = rootProxy.connect( _.overall.formCss )( FastEq.AnyRefEq ),
      )
    }
    .renderBackend[Backend]
    .build

  def apply(propsProxy: Props) = component( propsProxy )

}
