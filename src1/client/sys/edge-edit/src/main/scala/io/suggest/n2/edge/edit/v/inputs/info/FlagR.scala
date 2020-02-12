package io.suggest.n2.edge.edit.v.inputs.info

import com.materialui.{MuiCheckBox, MuiCheckBoxClasses, MuiCheckBoxProps, MuiFormControlLabel, MuiFormControlLabelProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.n2.edge.edit.m.FlagSet
import io.suggest.n2.edge.edit.v.EdgeEditCss
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React.Node
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.01.2020 23:21
  * Description: Legacy-флаг в info.
  */
class FlagR(
             crCtxProv: React.Context[MCommonReactCtx],
           ) {

  type Props_t = Option[Boolean]
  type Props = ModelProxy[Props_t]

  case class State(
                    isCheckedOptC: ReactConnectProxy[Option[Boolean]],
                  )


  class Backend($: BackendScope[Props, State]) {

    private val _onFlagChanged = ReactCommonUtil.cbFun2ToJsCb {
      (e: ReactEventFromInput, isChecked: Boolean) =>
        val isCheckedOpt = Option.when(!e.target.indeterminate)( isChecked )
        ReactDiodeUtil.dispatchOnProxyScopeCB( $, FlagSet(isCheckedOpt) )
    }

    def render(s: State): VdomElement = {
      val css = new MuiCheckBoxClasses {
        override val root = EdgeEditCss.inputLeft.htmlClass
      }
      MuiFormControlLabel {
        val _control = s.isCheckedOptC { isCheckedOptProxy =>
          val isCheckedOpt = isCheckedOptProxy.value
          MuiCheckBox {
            new MuiCheckBoxProps {
              override val indeterminate = isCheckedOpt.isEmpty
              override val checked = isCheckedOpt contains true
              @JSName("onChange")
              override val onChange2 = _onFlagChanged
              override val classes = css
            }
          }
        }

        val _label = crCtxProv.message( MsgCodes.`Flag.value` )

        new MuiFormControlLabelProps {
          override val control = _control.rawElement
          override val label: UndefOr[Node] = _label.rawNode
        }
      }

    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        isCheckedOptC = propsProxy.connect( identity ),
      )
    }
    .renderBackend[Backend]
    .build

}
