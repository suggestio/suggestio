package io.suggest.sc.v.search

import chandu0101.scalajs.react.components.materialui.{Mui, MuiFormControl, MuiFormControlClasses, MuiFormControlProps, MuiIconButton, MuiIconButtonClasses, MuiIconButtonProps, MuiInput, MuiInputClasses, MuiInputProps, MuiInputPropsMargins}
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.m.search.{MScSearchText, SearchTextChanged}
import io.suggest.sc.styl.{GetScCssF, ScCssStatic}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactEvent, ReactEventFromInput, ScalaComponent}
import org.scalajs.dom.raw.HTMLInputElement
import scalacss.ScalaCssReact._

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.07.17 12:50
  * Description: React-компонент поиского поля.
  * Скорее всего, его можно использовать через .wrap() вместо .connect.
  */
class STextR {

  type Props_t = MScSearchText
  type Props = ModelProxy[Props_t]


  class Backend( $: BackendScope[Props, Unit] ) {

    /** Происходит ввод текста в input. */
    private def _onInput(e: ReactEventFromInput): Callback = {
      val text = e.target.value
      dispatchOnProxyScopeCB($, SearchTextChanged(text))
    }
    private val _onInputJsF = ReactCommonUtil.cbFun1ToJsCb( _onInput )


    /** Callback для клика по кнопке очистики поискового поля. */
    private def _onClearClick(e: ReactEvent): Callback = {
      var cb = dispatchOnProxyScopeCB($, SearchTextChanged("", noWait = true))
      // focus на поле надо:
      if (_htmlInputRef.nonEmpty) {
        cb = cb >> Callback {
          for (htmlInput <- _htmlInputRef)
            htmlInput.focus()
        }
      }
      // И вернуть итоговый callback:
      cb
    }
    lazy val _onClearClickJsF = ReactCommonUtil.cbFun1ToJsCb( _onClearClick )


    /** Инстанс нативного элемента, чтобы фокусом отсюда управлять. */
    private var _htmlInputRef: Option[HTMLInputElement] = None
    /** Callback для перехвата ref'а DOM input-ноды. */
    private lazy val _htmlInputRefHandlerJsF: js.Function1[HTMLInputElement, Unit] = {
      el: HTMLInputElement =>
        _htmlInputRef = Some( el )
    }

    def render(propsProxy: Props): VdomElement = {
      val p = propsProxy.value

      // Рендер текстового поля с input'ом.
      val TextBarCSS = ScCssStatic.Search.TextBar
      val formCtlCss = new MuiFormControlClasses {
        override val root = TextBarCSS.inputFormControl.htmlClass
      }

      <.div(
        TextBarCSS.bar,

        MuiFormControl(
          new MuiFormControlProps {
            override val classes = formCtlCss
          }
        )(
          MuiInput {
            //val inputCss = new MuiInputClasses {
            //  override val root = TextBarCSS.inputRoot.htmlClass
            //}
            new MuiInputProps {
              //override val classes = inputCss
              override val `type` = HtmlConstants.Input.text
              override val onChange = _onInputJsF
              override val placeholder = Messages( MsgCodes.`Search.start.typing` )
              override val value = js.defined( p.query )
              override val margin = if (p.query.length > 15) MuiInputPropsMargins.dense else MuiInputPropsMargins.none
              // clear-кнопка:
              //override val endAdornment = clearBtnUndef
              override val inputRef: js.UndefOr[js.Function1[HTMLInputElement, _] | js.Object] =
                js.defined( _htmlInputRefHandlerJsF )
            }
          },

          // Кнопка быстрой очистки поля.
          MuiIconButton {
            val iconBtnCss = new MuiIconButtonClasses {
              override val root = {
                if (p.query.isEmpty) Css.Display.INVISIBLE
                else Css.Display.VISIBLE
              }
            }
            new MuiIconButtonProps {
              override val classes = iconBtnCss
              override val onClick = _onClearClickJsF
              override val disableRipple = true
            }
          }(
            Mui.SvgIcons.HighlightOffOutlined()()
          )

        )

      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(scSearchTextOptProxy: Props) = component( scSearchTextOptProxy )

}
