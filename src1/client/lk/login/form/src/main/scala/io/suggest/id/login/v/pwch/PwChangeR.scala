package io.suggest.id.login.v.pwch

import com.materialui.{MuiButton, MuiButtonProps, MuiButtonSizes, MuiDialog, MuiDialogActions, MuiDialogClasses, MuiDialogContent, MuiDialogMaxWidths, MuiDialogProps, MuiDialogTitle, MuiFormControl, MuiFormControlProps, MuiFormGroup, MuiFormGroupProps, MuiSnackBarContent, MuiSnackBarContentClasses, MuiSnackBarContentProps}
import diode.FastEq
import diode.data.Pot
import diode.react.ReactPot._
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.css.CssR
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.IdentConst
import io.suggest.id.login.m.pwch.MPwChangeRootS
import io.suggest.id.login.m.{PasswordBlur, RegNextClick, SetPassword}
import io.suggest.id.login.v.LoginFormCss
import io.suggest.id.login.v.stuff.{ErrorSnackR, LoginProgressR, TextFieldR}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.DiodeUtil.Implicits._
import japgolly.scalajs.react.{BackendScope, Callback, React, ReactEvent, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.07.19 19:10
  * Description: Компонент с формой смены пароля.
  */
class PwChangeR (
                  textFieldR            : TextFieldR,
                  pwNewR                : PwNewR,
                  loginProgressR        : LoginProgressR,
                  errorSnackR           : ErrorSnackR,
                  crCtxProv             : React.Context[MCommonReactCtx],
                  loginFormCssCtx       : React.Context[LoginFormCss],
                ) {

  // Зависим от корневой модели, чтобы иметь доступ к пошаренному инстансу pwNew.
  type Props_t = MPwChangeRootS
  type Props = ModelProxy[Props_t]


  case class State(
                    loginFormCssC           : ReactConnectProxy[LoginFormCss],
                    submitEnabledSomeC      : ReactConnectProxy[Some[Boolean]],
                    submitReqC              : ReactConnectProxy[Pot[None.type]],
                  )


  class Backend($: BackendScope[Props, State]) {

    private def _onSaveClick(e: ReactEvent): Callback = {
      ReactDiodeUtil.dispatchOnProxyScopeCB($, RegNextClick) >>
      ReactCommonUtil.preventDefaultCB(e)
    }

    def render(p: Props, s: State): VdomElement = {

      // Заголовок диалога
      val dialogTitle = MuiDialogTitle()(
        crCtxProv.message( MsgCodes.`Password.change` )
      )

      // Наполнение диалога.
      val dialogContent = MuiDialogContent()(

        MuiFormControl(
          new MuiFormControlProps {
            override val component = js.defined( <.fieldset.name )
          }
        )(

          MuiFormGroup(
            new MuiFormGroupProps {
              override val row = true
            }
          )(

            {
              val onBlurSome = Some( PasswordBlur )
              val mkActionSome = Some( SetPassword.apply _ )
              val placeHolder = ""
              p.wrap { mroot =>
                textFieldR.PropsVal(
                  state       = mroot.form.pwOld,
                  hasError    = false,
                  mkAction    = mkActionSome,
                  isPassword  = true,
                  inputName   = IdentConst.Login.PASSWORD_FN,
                  label       = MsgCodes.`Type.current.password`,
                  placeHolder = placeHolder,
                  onBlur      = onBlurSome,
                  disabled    = mroot.form.submitReq.isPending,
                )
              }(textFieldR.apply)(implicitly, textFieldR.EpwTextFieldPropsValFastEq)
            },

          ),

          // Поля для ввода и подтверждения нового пароля.
          p.wrap { mroot =>
            pwNewR.PropsVal(
              pwNew       = mroot.form.pwNew,
              reqPending  = mroot.form.submitReq.isPending,
              isNew       = true,
            )
          }(pwNewR.component.apply)(implicitly, pwNewR.PwNewRPropsValFastEq),

          {
            lazy val successMsg = crCtxProv.message( MsgCodes.`New.password.saved` )

            // Рендер сообщения о том, что новый пароль успешно сохранён.
            loginFormCssCtx.consume { lfCssCtx =>
              s.submitReqC { submitReqProxy =>
                val submitReq = submitReqProxy.value
                <.div(

                  // Прогресс-бар ожидания...
                  submitReqProxy.wrap { sr => OptionUtil.SomeBool(sr.isPending) }( loginProgressR.component.apply )(implicitly, FastEq.AnyRefEq),

                  // Сохранение удалось.
                  submitReq.renderReady { _ =>
                    val cssClasses = new MuiSnackBarContentClasses {
                      override val root = lfCssCtx.bgColorSuccess.htmlClass
                    }
                    MuiSnackBarContent {
                      new MuiSnackBarContentProps {
                        override val classes = cssClasses
                        override val message = successMsg.rawNode
                      }
                    }
                  },

                  // Рендер ошибки сохранения:
                  submitReqProxy.wrap(_.exceptionOrNull)( errorSnackR.component.apply )(implicitly, FastEq.AnyRefEq),

                )
              }
            }

          },

        )
      )

      val dialogActions = MuiDialogActions()(
        // Кнопка отмены:
        /*{
          // Сообщение кнопки "Отмена".
          val cancelMsg = commonReactCtxProv.consume { commonReactCtx =>
            commonReactCtx.messages( MsgCodes.`Cancel` )
          }
          s.isSubmitPendingSomeC { isSubmitPendingSomeProxy =>
            MuiButton(
              new MuiButtonProps {
                override val size     = MuiButtonSizes.small
                override val onClick  = _onCancelClickCbF
                override val `type`   = HtmlConstants.Input.button
                override val disabled = isSubmitPendingSomeProxy.value.value
              }
            )(
              cancelMsg,
            )
          }
        },*/

        // Кнопка сохранения нового пароля.
        {
          val saveMsg = crCtxProv.message( MsgCodes.`Save` )

          s.submitEnabledSomeC { submitEnableSomeProxy =>
            MuiButton(
              new MuiButtonProps {
                override val size     = MuiButtonSizes.small
                override val `type`   = HtmlConstants.Input.submit
                override val disabled = !submitEnableSomeProxy.value.value
              }
            )(
              saveMsg,
            )
          }
        },
      )

      val dia = loginFormCssCtx.consume { lfCss =>
        val diaCss = new MuiDialogClasses {
          override val root = lfCss.lkDiaRoot.htmlClass
        }
        MuiDialog(
          new MuiDialogProps {
            override val maxWidth = js.defined {
              MuiDialogMaxWidths.xs
            }
            override val open = true
            //override val onClose = _onCancelClickCbF
            // TODO disable* -- true на одинокой форме. false/undefined для формы, встраиваемой в выдачу.
            override val disableBackdropClick = true
            override val disableEscapeKeyDown = true
            override val classes = diaCss
          }
        )(
          dialogTitle,

          // Перехват сабмита в форме.
          <.form(
            ^.onSubmit ==> _onSaveClick,
            dialogContent,
            dialogActions,
          ),
        )
      }

      // Добавить внутренний контекст для CSS.
      s.loginFormCssC { loginFormCssProxy =>
        <.div(
          CssR.compProxied( loginFormCssProxy ),

          loginFormCssCtx.provide( loginFormCssProxy.value )(
            dia
          )
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        loginFormCssC = propsProxy.connect(_.formCss)( FastEq.AnyRefEq ),

        submitEnabledSomeC = propsProxy.connect { mroot =>
          OptionUtil.SomeBool( mroot.form.canSubmit )
        },

        submitReqC = propsProxy.connect(_.form.submitReq)( FastEq.AnyRefEq ),

      )
    }
    .renderBackend[Backend]
    .build

}
