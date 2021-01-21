package io.suggest.ad.edit.v.edit.strip

import com.materialui.{MuiFormControlLabel, MuiFormControlLabelClasses, MuiFormControlLabelProps, MuiInputValue_t, MuiLabelPlacements, MuiSelectProps, MuiTextField, MuiTextFieldProps}
import diode.react.ModelProxy
import io.suggest.ad.blk.{MBlockExpandMode, MBlockExpandModes}
import io.suggest.ad.edit.m.BlockExpand
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactEventFromInput, ScalaComponent}
import japgolly.univeq._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.11.17 10:57
  * Description: React-компонента галочки широкоформатного отображения.
  */
class ShowWideR(
                 lkAdEditCss        : LkAdEditCss,
               ) {

  type Props_t = Option[MBlockExpandMode]
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    private val emptyOptionValue = ""

    /** Реакция на изменение состояния галочки широкоформатного отображения. */
    private def _onClick(e: ReactEventFromInput): Callback = {
      val v = e.target.value
      val mbemOpt = OptionUtil.maybeOpt( v !=* emptyOptionValue ) {
        MBlockExpandModes.withValueOpt( v )
      }
      ReactDiodeUtil.dispatchOnProxyScopeCB($, BlockExpand(mbemOpt))
    }
    private val _onClickJsCbF = ReactCommonUtil.cbFun1ToJsCb( _onClick )


    def render(propsOptProxy: Props): VdomElement = {
      val propsOpt = propsOptProxy.value
      val currSelectValue =
        propsOpt.fold(emptyOptionValue)( _.value ): MuiInputValue_t

      val _selectProps = new MuiSelectProps {
        override val native = true
        override val variant = MuiTextField.Variants.standard
      }

      val select = MuiTextField(
        new MuiTextFieldProps {
          override val select = true
          override val SelectProps = _selectProps
          override val value = js.defined( currSelectValue )
          override val onChange = _onClickJsCbF
          override val variant = MuiTextField.Variants.standard
        }
      )(
        <.option(
          ^.value := emptyOptionValue,
          Messages( MsgCodes.`Dont.expand` ),
        ),
        MBlockExpandModes.values.toVdomArray { mbem =>
          <.option(
            ^.key := mbem.value,
            ^.value := mbem.value,
            Messages( mbem.msgCode ),
          )
        },

      )

      MuiFormControlLabel {
        val cssClasses = new MuiFormControlLabelClasses {
          override val root = lkAdEditCss.WhControls.marginLeft0.htmlClass
          override val label = lkAdEditCss.WhControls.marginRight40.htmlClass
        }
        new MuiFormControlLabelProps {
          override val control        = select.rawElement
          override val label          = Messages( MsgCodes.`Expand` ).rawNode
          override val labelPlacement = MuiLabelPlacements.start
          override val classes        = cssClasses
        }
      }

    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsOptProxy: Props) = component(propsOptProxy)

}
