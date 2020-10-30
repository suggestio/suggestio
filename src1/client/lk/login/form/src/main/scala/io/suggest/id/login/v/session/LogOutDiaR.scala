package io.suggest.id.login.v.session

import com.materialui.{MuiButton, MuiButtonProps, MuiButtonSizes, MuiButtonVariants, MuiDialog, MuiDialogActions, MuiDialogClasses, MuiDialogContent, MuiDialogContentText, MuiDialogContentTextProps, MuiDialogProps, MuiLinearProgress, MuiLinearProgressProps, MuiProgressVariants, MuiTypoGraphyAligns, MuiTypoGraphyColors, MuiTypoGraphyVariants}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.login.m.LogoutConfirm
import io.suggest.id.login.m.session.MLogOutDia
import io.suggest.lk.r.plat.{PlatformComponents, PlatformCssStatic}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sjs.common.empty.JsOptionUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import ReactCommonUtil.Implicits._
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants._
import io.suggest.spa.OptFastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.09.2020 17:18
  * Description: Компонент диалога выхода из системы.
  */
class LogOutDiaR(
                  platformComponents  : PlatformComponents,
                  platfromCss         : () => PlatformCssStatic,
                  crCtxP              : React.Context[MCommonReactCtx],
                ) {

  type Props_t = Option[MLogOutDia]
  type Props = ModelProxy[Props_t]


  case class State(
                    isVisibleSomeC    : ReactConnectProxy[Some[Boolean]],
                    isPendingSomeC    : ReactConnectProxy[Some[Boolean]],
                    exceptionOptC     : ReactConnectProxy[Option[Throwable]],
                  )

  class Backend( $: BackendScope[Props, State] ) {

    private def _confirm(isLogout: Boolean) = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, LogoutConfirm( isLogout ) )
    }

    /** Сокрытие диалога логаута. */
    private lazy val _onClose = _confirm(false)

    /** Подтверждение лог-аута. */
    private lazy val _onLogoutConfirmed = _confirm(true)


    def render(s: State): VdomElement = {
      val platCss = platfromCss()

      val chs = crCtxP.consume { crCtx =>


        React.Fragment(
          // Заголовок
          platformComponents.diaTitle( Nil )(
            platformComponents.diaTitleText(
              crCtx.messages( MsgCodes.`Logout.account` ),
            ),
          ),

          // Содержимое диалога: вопрос-подтверждение выхода.
          MuiDialogContent()(

            // Текст вопроса:
            MuiDialogContentText(
              new MuiDialogContentTextProps {
                override val align = MuiTypoGraphyAligns.center
              }
            )(
              crCtx.messages( MsgCodes.`Are.you.sure.to.logout.account` ),
            ),

            // Вывод ошибки запроса разлогина:
            s.exceptionOptC { exceptionOptProxy =>
              exceptionOptProxy.value.whenDefinedEl { ex =>
                MuiDialogContentText(
                  new MuiDialogContentTextProps {
                    override val variant = MuiTypoGraphyVariants.caption
                    override val color = MuiTypoGraphyColors.error
                  }
                )(
                  crCtx.messages( MsgCodes.`Error` ),
                  COLON, SPACE,
                  ex.getMessage,
                  SPACE,
                  `(`, ex.getClass.getSimpleName, `)`,
                )
              }
            },

            // Если pending, то отрендерить progress:
            s.isPendingSomeC { isPendingSomeProxy =>
              val isPending = isPendingSomeProxy.value.value
              <.span(
                if (isPending) ^.visibility.visible else ^.visibility.hidden,
                MuiLinearProgress(
                  new MuiLinearProgressProps {
                    override val variant = if (isPending) MuiProgressVariants.indeterminate else MuiProgressVariants.determinate
                    override val value = JsOptionUtil.maybeDefined(!isPending)(0)
                  }
                )
              )
            },

          ),

          // Кнопки внизу.
          MuiDialogActions(
            platformComponents.diaActionsProps()(platCss)
          )(

            // Кнопка отмены:
            MuiButton(
              new MuiButtonProps {
                override val size = MuiButtonSizes.large
                override val variant = MuiButtonVariants.text
                override val onClick = _onClose
              }
            )(
              crCtx.messages( MsgCodes.`Cancel` ),
            ),

            // Кнопка подтверждения выхода
            {
              val logOutMsg = crCtx.messages( MsgCodes.`Logout` )
              s.isPendingSomeC { isPendingSomeProxy =>
                MuiButton(
                  new MuiButtonProps {
                    override val size = MuiButtonSizes.large
                    override val variant = MuiButtonVariants.text
                    override val onClick = _onLogoutConfirmed
                    override val disabled = isPendingSomeProxy.value.value
                  }
                )(
                  logOutMsg,
                )
              }
            },

          ),

        )
      }

      s.isVisibleSomeC { isVisibleSomeProxy =>
        val isVisible = isVisibleSomeProxy.value.value

        MuiDialog {
          val diaCss = new MuiDialogClasses {
            override val paper = platCss.Dialogs.paper.htmlClass
          }
          new MuiDialogProps {
            override val open = isVisible
            override val classes = diaCss
            override val onClose = _onClose
          }
        } (
          chs,
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        isVisibleSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.nonEmpty )
        },

        isPendingSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.exists(_.req.isPending) )
        },

        exceptionOptC = propsProxy.connect { props =>
          props.flatMap(_.req.exceptionOption)
        }( OptFastEq.Plain ),
      )
    }
    .renderBackend[Backend]
    .build

}
