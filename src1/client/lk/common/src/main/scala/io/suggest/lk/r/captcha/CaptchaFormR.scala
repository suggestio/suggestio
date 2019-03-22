package io.suggest.lk.r.captcha

import chandu0101.scalajs.react.components.materialui.{Mui, MuiIconButton, MuiIconButtonProps, MuiInputValue_t, MuiTextField, MuiTextFieldProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.captcha.MCaptchaCookiePath
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.m.{CaptchaInit, CaptchaInputBlur, CaptchaTyped, MTextFieldS}
import io.suggest.lk.m.captcha.MCaptchaS
import io.suggest.routes.routes
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react.{BackendScope, Callback, React, ReactEvent, ReactEventFromInput, ReactFocusEvent, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.03.19 10:06
  * Description: React-компонент ввода капчи. Монтируется через wrap(), обладая внутренним состоянием.
  */
class CaptchaFormR(
                    commonReactCtxProv    : React.Context[MCommonReactCtx],
                  ) {

  case class PropsVal(
                       captcha      : MCaptchaS,
                       cookiePath   : MCaptchaCookiePath,
                     )

  implicit object CaptchaFormRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.captcha      ===* b.captcha) &&
      (a.cookiePath   ===* b.cookiePath)
    }
  }


  case class CaptchaImgRenderArgs(
                                   captchaId    : Option[String],
                                   cookiePath   : MCaptchaCookiePath,
                                 )
  implicit object CaptchaImgRenderArgsFastEq extends FastEq[CaptchaImgRenderArgs] {
    override def eqv(a: CaptchaImgRenderArgs, b: CaptchaImgRenderArgs): Boolean = {
      (a.captchaId ===* b.captchaId) &&
      (a.cookiePath ===* b.cookiePath)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]

  case class State(
                    isShownSomeC              : ReactConnectProxy[Some[Boolean]],
                    typedC                    : ReactConnectProxy[MTextFieldS],
                    captchaImgRenderArgsC     : ReactConnectProxy[CaptchaImgRenderArgs],
                    reloadPendingSomeC        : ReactConnectProxy[Some[Boolean]],
                  )


  class Backend( $: BackendScope[Props, State] ) {

    private def _onChangeTyped(e: ReactEventFromInput): Callback = {
      val typed2 = e.target.value
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { propsProxy: Props =>
        CaptchaTyped(
          captchaId = propsProxy.value.captcha.captchaId.getOrElse(""),
          typed     = typed2,
        )
      }
    }
    private val _onTypedChangeCbF = ReactCommonUtil.cbFun1ToJsCb( _onChangeTyped )


    private def _onReloadClick(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB($, CaptchaInit)
    private val _onReloadClickCbF = ReactCommonUtil.cbFun1ToJsCb( _onReloadClick )


    private def _onInputBlur(e: ReactFocusEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB($, CaptchaInputBlur)
    private val _onInputBlurCbF = ReactCommonUtil.cbFun1ToJsCb( _onInputBlur )


    def render(props: Props, s: State): VdomElement = {

      // Рендер картинки капчи:
      val img = s.captchaImgRenderArgsC { imgArgsProxy =>
        val imgArgs = imgArgsProxy.value
        imgArgs.captchaId.whenDefinedEl { captchaId =>
          <.img(
            ^.src := {
              val route = routes.controllers.Img.getCaptchaImg(
                captchaId   = captchaId,
                cookiePath  = imgArgs.cookiePath.value,
              )
              route.url
            }
          )
        }
      }

      // Рендер инпута для ввода капчи:
      val input = commonReactCtxProv.consume { commonReactCtx =>
        val placeHolderText = commonReactCtx.messages( MsgCodes.`Input.text.from.picture` )
        s.typedC { typedProxy =>
          val typed = typedProxy.value
          MuiTextField(
            new MuiTextFieldProps {
              override val onChange = _onTypedChangeCbF
              override val value = js.defined {
                typed.value: MuiInputValue_t
              }
              override val placeholder  = placeHolderText
              override val `type`       = HtmlConstants.Input.text
              override val error        = !typed.isValid
              override val onBlur       = _onInputBlurCbF
            }
          )
        }
      }

      // Кнопка обновления капчи:
      val reloadBtn = {
        val reloadIcon = Mui.SvgIcons.Refresh()()
        s.reloadPendingSomeC { reloadPendingSomeProxy =>
          MuiIconButton(
            new MuiIconButtonProps {
              override val disabled = reloadPendingSomeProxy.value.value
              override val onClick = _onReloadClickCbF
            }
          )(
            reloadIcon
          )
        }
      }

      // Рендер общего контейнера:
      s.isShownSomeC { isShownSomeProxy =>
        ReactCommonUtil.maybeEl( isShownSomeProxy.value.value ) {
          <.div(
            img,
            input,
            reloadBtn,
          )
        }
      }

    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        isShownSomeC          = propsProxy.connect(_.captcha.isShownSome)( FastEqUtil.RefValFastEq ),
        typedC                = propsProxy.connect(_.captcha.typed)( MTextFieldS.MEpwTextFieldSFastEq ),
        captchaImgRenderArgsC = propsProxy.connect { props =>
          CaptchaImgRenderArgs(
            captchaId   = props.captcha.captchaId,
            cookiePath  = props.cookiePath,
          )
        }( CaptchaImgRenderArgsFastEq ),
        reloadPendingSomeC    = propsProxy.connect(_.captcha.reloadPendingSome)( FastEqUtil.RefValFastEq )
      )
    }
    .renderBackend[Backend]
    .build


  def apply( propsValProxy: Props ) = component( propsValProxy )

}
