package io.suggest.lk.nodes.form.r.tree

import com.materialui.{MuiListItem, MuiListItemProps, MuiListItemSecondaryAction, MuiListItemText, MuiSwitch, MuiSwitchProps}
import diode.data.Pot
import diode.react.ModelProxy
import io.suggest.i18n.MCommonReactCtx
import io.suggest.lk.nodes.form.m.LkNodesAction
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.FastEqUtil
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.08.2020 12:12
  * Description: Галочки (свитчи) размещения карточки на узле.
  */
final class NodeAdvRowR(
                         crCtxP               : React.Context[MCommonReactCtx],
                       ) {

  case class PropsVal(
                       flag           : Pot[Boolean],
                       msgCode        : String,
                       onChange       : Boolean => LkNodesAction,
                     )
  implicit def pvFeq = FastEqUtil[PropsVal] { (a, b) =>
    (a.flag ===* b.flag) &&
    (a.msgCode ===* b.msgCode)
  }

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Props_t]) {

    private lazy val _onChangeCbF = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val isChecked = e.target.checked
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { props: Props =>
        props.value.onChange( isChecked )
      }
    }

    def render(s: Props_t): VdomElement = {
      MuiListItem(
        new MuiListItemProps {
          override val button = true
        }
      )(
        // Заголовочек:
        MuiListItemText()(
          crCtxP.message( s.msgCode ),
        ),

        MuiListItemSecondaryAction()(
          MuiSwitch(
            new MuiSwitchProps {
              override val checked  = s.flag contains true
              override val disabled = s.flag.isPending
              override val onChange = _onChangeCbF
            }
          )
        ),
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate(pvFeq) )
    .build

}
