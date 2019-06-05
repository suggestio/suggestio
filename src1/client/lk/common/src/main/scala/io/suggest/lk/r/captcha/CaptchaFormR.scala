package io.suggest.lk.r.captcha

import chandu0101.scalajs.react.components.materialui.{Mui, MuiIconButton, MuiIconButtonProps, MuiInputValue_t, MuiLinearProgress, MuiTextField, MuiTextFieldProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.m.{CaptchaInit, CaptchaInputBlur, CaptchaTyped, MTextFieldS}
import io.suggest.lk.m.captcha.MCaptchaS
import io.suggest.spa.{FastEqUtil, OptFastEq}
import japgolly.scalajs.react.{BackendScope, Callback, React, ReactEvent, ReactEventFromInput, ReactFocusEvent, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
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
                       captcha      : Option[MCaptchaS],
                       disabled     : Boolean = false,
                     ) {

    lazy val reloadPendingSome: Some[Boolean] =
      Some( disabled || captcha.exists(_.req.isPending) )

  }

  implicit object CaptchaFormRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.disabled      ==* b.disabled) &&
      (a.captcha      ===* b.captcha)
    }
  }

  case class TypedProps(
                         typedOpt: Option[MTextFieldS],
                         disabled: Boolean,
                       )
  implicit object TypedPropsFastEq extends FastEq[TypedProps] {
    val typedOptFeq = OptFastEq.Wrapped(MTextFieldS.MEpwTextFieldSFastEq)
    override def eqv(a: TypedProps, b: TypedProps): Boolean = {
      typedOptFeq.eqv(a.typedOpt, b.typedOpt) &&
      a.disabled ==* b.disabled
    }
  }

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]

  case class State(
                    isShownC                  : ReactConnectProxy[Option[MCaptchaS]],
                    captchaTypedC             : ReactConnectProxy[TypedProps],
                    captchaImgUrlC            : ReactConnectProxy[Option[String]],
                    reloadPendingSomeC        : ReactConnectProxy[Some[Boolean]],
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

      // Рендер инпута для ввода капчи:
      val input = commonReactCtxProv.consume { commonReactCtx =>
        val placeHolderText = commonReactCtx.messages( MsgCodes.`Input.text.from.picture` )
        s.captchaTypedC { captchaProxy =>
          val tp = captchaProxy.value
          tp.typedOpt.whenDefinedEl { captchaTypedS =>
            MuiTextField(
              new MuiTextFieldProps {
                override val onChange = _onTypedChangeCbF
                override val value = js.defined {
                  captchaTypedS.value: MuiInputValue_t
                }
                override val placeholder  = placeHolderText
                override val `type`       = HtmlConstants.Input.text
                override val error        = !captchaTypedS.isValid
                override val onBlur       = _onInputBlurCbF
                override val disabled     = tp.disabled
              }
            )
          }
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
        isShownC = propsProxy.connect(_.captcha)( OptFastEq.IsEmptyEq ),
        captchaTypedC         = propsProxy.connect { props =>
          TypedProps(
            typedOpt = for (c <- props.captcha) yield c.typed,
            disabled = props.disabled,
          )
        }( TypedPropsFastEq ),

        captchaImgUrlC        = propsProxy.connect(_.captcha.flatMap(_.captchaImgUrlOpt))( OptFastEq.Plain ),

        reloadPendingSomeC    = propsProxy.connect( _.reloadPendingSome )( FastEqUtil.RefValFastEq )
      )
    }
    .renderBackend[Backend]
    .build


  def apply( propsValProxy: Props ) = component( propsValProxy )

}
