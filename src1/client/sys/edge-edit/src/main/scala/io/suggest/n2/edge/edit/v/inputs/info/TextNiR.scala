package io.suggest.n2.edge.edit.v.inputs.info

import com.materialui.{MuiCheckBox, MuiCheckBoxClasses, MuiCheckBoxProps, MuiColorTypes, MuiFormControlClasses, MuiFormControlLabel, MuiFormControlLabelProps, MuiTextField, MuiTextFieldProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.n2.edge.edit.m.TextNiSet
import io.suggest.n2.edge.edit.v.EdgeEditCss
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.2020 12:19
  * Description: Редактирование неиндексируемого текста в эдже.
  */
class TextNiR(
               crCtxProv            : React.Context[MCommonReactCtx],
             ) {

  type Props_t = Option[String]
  type Props = ModelProxy[Props_t]

  case class State(
                    isEnabledSomeC      : ReactConnectProxy[Some[Boolean]],
                    valueOptC           : ReactConnectProxy[Option[String]],
                  )

  class Backend( $: BackendScope[Props, State] ) {

    private def _doDispatch(textNiOpt: Option[String]): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, TextNiSet(textNiOpt) )

    private val _onCheckBoxChange = ReactCommonUtil.cbFun2ToJsCb { (_: ReactEventFromInput, checked: Boolean) =>
      val textNiOpt = Option.when( checked )("")
      _doDispatch( textNiOpt )
    }

    private val _onTextEdit = ReactCommonUtil.cbFun1ToJsCb { (e: ReactEventFromInput) =>
      val text = e.target.value
      _doDispatch( Option(text) )
    }


    def render( s: State ): VdomElement = {
      val emptyStr = ""

      <.div(

        // Галочка отображения редактора неиндексируемого текста.
        MuiFormControlLabel {
          val css = new MuiCheckBoxClasses {
            override val root = EdgeEditCss.inputLeft.htmlClass
          }
          val _checkBox = s.isEnabledSomeC { isEnabledSomeProxy =>
            MuiCheckBox(
              new MuiCheckBoxProps {
                override val checked = isEnabledSomeProxy.value.value
                @JSName("onChange")
                override val onChange2 = _onCheckBoxChange
                override val classes = css
                override val color = MuiColorTypes.secondary
              }
            )
          }

          val _checkBoxLabel = crCtxProv.message( MsgCodes.`Not.indexed.text` )

          new MuiFormControlLabelProps {
            override val control  = _checkBox.rawElement
            override val label    = _checkBoxLabel.rawNode
          }
        },

        crCtxProv.consume { crCtx =>
          s.valueOptC { valueOptProxy =>
            val _valueOpt = valueOptProxy.value
            val _value = _valueOpt getOrElse emptyStr

            ReactCommonUtil.maybeEl( _valueOpt.nonEmpty ) {
              val css = new MuiFormControlClasses {
                override val root = EdgeEditCss.inputLeft.htmlClass
              }
              // textarea с вводом текста.
              MuiTextField(
                new MuiTextFieldProps {
                  override val multiline  = true
                  override val minRows       = js.defined( 2 )
                  override val value      = _value
                  override val onChange   = _onTextEdit
                  override val variant    = MuiTextField.Variants.outlined
                  override val autoFocus  = true
                  override val placeholder = crCtx.messages( MsgCodes.`Type.text...` )
                  override val classes    = css
                }
              )()
            }
          }
        },

      )
    }
  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        isEnabledSomeC = propsProxy.connect { textOpt =>
          OptionUtil.SomeBool( textOpt.nonEmpty )
        },
        valueOptC = propsProxy.connect( identity(_) ),
      )
    }
    .renderBackend[Backend]
    .build

}
