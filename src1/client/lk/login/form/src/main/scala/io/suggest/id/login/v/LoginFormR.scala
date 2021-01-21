package io.suggest.id.login.v

import com.materialui.{MuiDialog, MuiDialogClasses, MuiDialogContent, MuiDialogMaxWidths, MuiDialogProps, MuiDialogTitle, MuiTab, MuiTabProps, MuiTabs, MuiTabsClasses, MuiTabsProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.css.CssR
import io.suggest.i18n.MCommonReactCtx
import io.suggest.id.login.{MLoginTab, MLoginTabs}
import io.suggest.id.login.m._
import io.suggest.id.login.m.epw.MEpwLoginS
import io.suggest.id.login.m.ext.MExtLoginFormS
import io.suggest.id.login.v.epw.EpwFormR
import io.suggest.id.login.v.ext.ExtFormR
import io.suggest.id.login.v.reg.RegR
import io.suggest.lk.u.MaterialUiUtil
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sjs.common.empty.JsOptionUtil
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
                  regR                  : RegR,
                  crCtxProv             : React.Context[MCommonReactCtx],
                  loginFormCssCtx       : React.Context[LoginFormCss],
                  lfDiConf              : LoginFormDiConfig,
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
      val labelText = crCtxProv.message( tab.msgCode )

      new MuiTabProps {
        override val value = js.defined( tab.value )
        override val label = js.defined {
          labelText.rawNode
        }
      }
    }
  }


  class Backend($: BackendScope[Props, State]) {

    private val _onTabChangedCbF = ReactCommonUtil.cbFun2ToJsCb { (_: ReactEventFromHtml, newValue: js.Any) =>
      val newTab = MLoginTabs.withValue( newValue.asInstanceOf[Int] )
      ReactDiodeUtil.dispatchOnProxyScopeCB($, SwitсhLoginTab(newTab) )
    }

    private lazy val _onLoginCloseCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      Callback.lazily( lfDiConf.onClose() getOrElse Callback.empty )
    }


    def render(p: Props, s: State): VdomElement = {
      // Форма логина через внешние сервисы.
      lazy val extLogin = p.wrap(_.ext)( extFormR.component.apply )( implicitly, MExtLoginFormS.MExtLoginFormSFastEq )

      // Содержимое вкладки входа по логину и паролю:
      lazy val pwLogin = p.wrap(_.epw)( epwFormR.component.apply )( implicitly, MEpwLoginS.MEpwLoginSFastEq )

      // Вкладка регистрации по email и паролю.
      lazy val reg = regR.component(p)


      // Содержимое табов:
      val tabsContents = s.currTabC { currTabProxy =>
        // TODO Запилить react-swipeable-views, как в примерах MuiTabs.
        currTabProxy.value match {
          case MLoginTabs.EpwLogin          => pwLogin
          case MLoginTabs.Reg               => reg
          case MLoginTabs.Ext               => extLogin
        }
      }

      // кнопка таба EmailPw-логина:
      val loginTabBtn  = _tabBtn( MLoginTabs.EpwLogin )
      //val extTabBtn       = _tabBtn( MLoginTabs.Ext )
      val regTabBtn    = _tabBtn( MLoginTabs.Reg )

      // Заголовок диалога
      val dialogTitle = MuiDialogTitle()(
        // Список табов:
        loginFormCssCtx.consume { loginFormCss =>
          s.currTabC { currTabProxy =>
            MuiTabs {
              val tabsCss = new MuiTabsClasses {
                override val flexContainer = loginFormCss.tabsCont.htmlClass
              }
              new MuiTabsProps {
                override val value = js.defined( currTabProxy.value.value )
                @JSName("onChange")
                override val onTabChanged = _onTabChangedCbF
                override val classes = tabsCss
              }
            } (
              //extTabBtn,
              loginTabBtn,
              regTabBtn,
            )
          }
        }
      )

      // Наполнение диалога.
      val dialogContent = MuiDialogContent()(
        tabsContents,
      )

      // Весь диалог формы логина:
      val notCloseable = lfDiConf.onClose().isEmpty
      val allForm = loginFormCssCtx.consume { loginFormCss =>
        s.visibleSomeC { visibleSomeProxy =>
          MuiDialog {
            val diaCss = new MuiDialogClasses {
              override val paper = loginFormCss.diaWindow.htmlClass
            }
            new MuiDialogProps {
              override val maxWidth = js.defined {
                MuiDialogMaxWidths.xs
              }
              override val open = visibleSomeProxy.value.value
              override val onClose = JsOptionUtil.maybeDefined(!notCloseable)( _onLoginCloseCbF )
              // disable* -- true на одинокой форме. false/undefined для формы, встраиваемой в выдачу.
              //override val disableBackdropClick = notCloseable // mui v5 - не требуется,
              override val disableEscapeKeyDown = notCloseable
              override val classes = diaCss
            }
          } (
            dialogTitle,
            dialogContent,
          )
        }
      }

      // Добавить внутренний контекст для CSS.
      val result = s.loginFormCssC { loginFormCssProxy =>
        <.div(
          CssR.compProxied( loginFormCssProxy ),

          loginFormCssCtx.provide( loginFormCssProxy.value )(
            allForm
          ),
        )
      }

      MaterialUiUtil.postprocessTopLevel( result )
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

}
