package io.suggest.sc.v.search

import com.materialui.{Mui, MuiFormControl, MuiFormControlClasses, MuiFormControlProps, MuiIconButton, MuiIconButtonClasses, MuiIconButtonProps, MuiInput, MuiInputClasses, MuiInputProps, MuiInputPropsMargins}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.m.MScReactCtx
import io.suggest.sc.m.search.{MScSearchText, SearchTextChanged}
import io.suggest.sc.v.styl.ScCssStatic
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, React, ReactEvent, ReactEventFromInput, ScalaComponent}
import org.scalajs.dom.raw.HTMLInputElement
import scalacss.ScalaCssReact._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.07.17 12:50
  * Description: wrap-компонент поиского поля.
  * Скорее всего, его можно использовать через .wrap() вместо .connect.
  */
class STextR(
              crCtxProv     : React.Context[MCommonReactCtx],
              scReactCtxP   : React.Context[MScReactCtx],
            ) {

  type Props_t = MScSearchText
  type Props = ModelProxy[Props_t]


  case class State(
                    queryC                : ReactConnectProxy[String],
                    queryEmptySomeC       : ReactConnectProxy[Some[Boolean]],
                  )

  class Backend( $: BackendScope[Props, State] ) {

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
    private lazy val _onClearClickJsF = ReactCommonUtil.cbFun1ToJsCb( _onClearClick )


    /** Инстанс нативного элемента, чтобы фокусом отсюда управлять. */
    private var _htmlInputRef: Option[HTMLInputElement] = None
    /** Callback для перехвата ref'а DOM input-ноды. */
    private lazy val _htmlInputRefHandlerJsF: js.Function1[HTMLInputElement, Unit] = {
      el: HTMLInputElement =>
        _htmlInputRef = Some( el )
    }

    def render(s: State): VdomElement = {

      // Рендер текстового поля с input'ом.
      val TextBarCSS = ScCssStatic.Search.TextBar
      val formCtlCss = new MuiFormControlClasses {
        override val root = TextBarCSS.inputFormControl.htmlClass
      }
      val clearIcon = Mui.SvgIcons.HighlightOffOutlined()()

      <.div(
        TextBarCSS.bar,

        MuiFormControl(
          new MuiFormControlProps {
            override val classes = formCtlCss
          }
        )(
          // Текстовое поле поисковой строки:
          crCtxProv.consume { crCtx =>
            val startSearchTypingMsg = crCtx.messages( MsgCodes.`Search.start.typing` )

            scReactCtxP.consume { scReactCtx =>
              s.queryC { queryProxy =>
                MuiInput {
                  val query = queryProxy.value
                  val inputCss = new MuiInputClasses {
                    override val underline = scReactCtx.scCss.Search.TextBar.underline.htmlClass
                  }
                  new MuiInputProps {
                    override val classes = inputCss
                    override val `type` = HtmlConstants.Input.text
                    override val onChange = _onInputJsF
                    override val placeholder = startSearchTypingMsg
                    override val value = js.defined( query )
                    override val margin = if (query.length > 15) MuiInputPropsMargins.dense else MuiInputPropsMargins.none
                    // clear-кнопка:
                    //override val endAdornment = clearBtnUndef
                    override val inputRef = js.defined( _htmlInputRefHandlerJsF )
                  }
                }
              }
            }
          },

          // Крестик быстрой очистки поля поиска:
          s.queryEmptySomeC { queryEmptySomeProxy =>
            // Кнопка быстрой очистки поля.
            MuiIconButton {
              val iconBtnCss = new MuiIconButtonClasses {
                override val root = {
                  if (queryEmptySomeProxy.value.value) Css.Display.INVISIBLE
                  else Css.Display.VISIBLE
                }
              }
              new MuiIconButtonProps {
                override val classes = iconBtnCss
                override val onClick = _onClearClickJsF
                override val disableRipple = true
              }
            } (
              clearIcon
            )
          },

        )

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        queryC = propsProxy.connect( _.query ),

        queryEmptySomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.query.isEmpty )
        },

      )
    }
    .renderBackend[Backend]
    .build

}
