package io.suggest.n2.edge.edit.v.inputs.info

import com.materialui.{MuiCheckBox, MuiCheckBoxClasses, MuiCheckBoxProps, MuiColorTypes, MuiFormControlLabel, MuiFormControlLabelProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.n2.edge.edit.m.FlagSet
import io.suggest.n2.edge.edit.v.EdgeEditCss
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

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

    private val _onFlagChanged = ReactCommonUtil.cbFun1ToJsCb {
      e: ReactEventFromInput =>
        val isCheckedOpt = Option.when( !e.target.indeterminate )( e.target.checked )
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
              override val onChange = _onFlagChanged
              override val classes = css
              override val color = MuiColorTypes.secondary
            }
          }
        }

        val _label = crCtxProv.message( MsgCodes.`Flag.value` )

        new MuiFormControlLabelProps {
          override val control = _control.rawElement
          override val label = _label.rawNode
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
