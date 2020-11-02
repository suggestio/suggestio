package io.suggest.lk.r.captcha

import com.materialui.{Mui, MuiIconButton, MuiIconButtonProps, MuiInputValue_t, MuiLinearProgress, MuiTextField, MuiTextFieldProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.m.{CaptchaInit, CaptchaInputBlur, CaptchaTyped}
import io.suggest.lk.m.captcha.MCaptchaS
import io.suggest.lk.m.input.MTextFieldExtS
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react.{BackendScope, Callback, React, ReactEvent, ReactEventFromInput, ReactFocusEvent, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import io.suggest.common.empty.OptionUtil.BoolOptOps
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
                    crCtxProv    : React.Context[MCommonReactCtx],
                  ) {

  import CaptchaFormR._

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]

  case class State(
                    isShownC                  : ReactConnectProxy[Option[PropsVal]],
                    captchaTypedC             : ReactConnectProxy[Option[MTextFieldExtS]],
                    captchaImgUrlC            : ReactConnectProxy[Option[String]],
                    reloadPendingSomeC        : ReactConnectProxy[Option[Boolean]],
                  )


  class Backend( $: BackendScope[Props, State] ) {

    private def _onChangeTyped(e: ReactEventFromInput): Callback = {
      val typed2 = e.target.value
      ReactDiodeUtil.dispatchOnProxyScopeCB($, CaptchaTyped(typed = typed2) )
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
      val img = s.captchaImgUrlC { imgArgsProxy =>
        imgArgsProxy
          .value
          .fold[VdomElement] {
            MuiLinearProgress()
          } { captchaUrl =>
            <.img(
              ^.src := captchaUrl
            )
          }
      }

      val placeHolderText = crCtxProv.message( MsgCodes.`Input.text.from.picture` )

      // Рендер инпута для ввода капчи:
      val input = s.captchaTypedC { captchaProxy =>
        captchaProxy.value.whenDefinedEl { captchaS =>
          MuiTextField(
            new MuiTextFieldProps {
              override val onChange = _onTypedChangeCbF
              override val value = js.defined {
                captchaS.typed.value: MuiInputValue_t
              }
              override val label        = placeHolderText.rawNode
              override val `type`       = HtmlConstants.Input.tel
              override val error        = !captchaS.typed.isValid
              override val onBlur       = _onInputBlurCbF
              override val disabled     = captchaS.disabled
            }
          )()
        }
      }

      // Кнопка обновления капчи:
      val reloadBtn = {
        val reloadIcon = Mui.SvgIcons.Refresh()()
        s.reloadPendingSomeC { reloadPendingSomeProxy =>
          MuiIconButton(
            new MuiIconButtonProps {
              override val disabled = reloadPendingSomeProxy.value.getOrElseTrue
              override val onClick = _onReloadClickCbF
            }
          )(
            reloadIcon
          )
        }
      }

      // Рендер общего контейнера:
      s.isShownC { isShownProxy =>
        isShownProxy.value.whenDefinedEl { _ =>
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
        isShownC              = propsProxy.connect(identity)( OptFastEq.IsEmptyEq ),

        captchaTypedC         = propsProxy.connect { props =>
          for (c <- props) yield {
            MTextFieldExtS(
              typed = c.captcha.typed,
              disabled = c.disabled,
            )
          }
        }( OptFastEq.Wrapped(MTextFieldExtS.MTextFieldExtSFastEq) ),

        captchaImgUrlC        = propsProxy.connect(_.flatMap(_.captcha.captchaImgUrlOpt))( OptFastEq.Plain ),

        reloadPendingSomeC    = propsProxy.connect( _.map(_.reloadPending) )( FastEq.ValueEq )
      )
    }
    .renderBackend[Backend]
    .build


  def apply( propsValProxy: Props ) = component( propsValProxy )

}


object CaptchaFormR {

  case class PropsVal(
                       captcha      : MCaptchaS,
                       disabled     : Boolean    = false,
                     ) {

    def reloadPending: Boolean =
      disabled || captcha.contentReq.isPending

  }

  implicit object CaptchaFormRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.disabled      ==* b.disabled) &&
      (a.captcha      ===* b.captcha)
    }
  }

}
