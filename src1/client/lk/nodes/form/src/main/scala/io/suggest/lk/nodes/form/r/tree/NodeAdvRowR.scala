package io.suggest.lk.nodes.form.r.tree

import com.materialui.{MuiListItem, MuiListItemProps, MuiListItemSecondaryAction, MuiListItemText, MuiSwitch, MuiSwitchProps}
import diode.data.Pot
import diode.react.ModelProxy
import io.suggest.i18n.MCommonReactCtx
import io.suggest.lk.nodes.form.r.LkNodesFormCss
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
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
                         treeStuffR           : TreeStuffR,
                         lkNodesFormCssP      : React.Context[LkNodesFormCss],
                         crCtxP               : React.Context[MCommonReactCtx],
                       ) {

  case class PropsVal(
                       flag           : Pot[Boolean],
                       msgCode        : String,
                       onChange       : Boolean => Callback,
                     )
  implicit val pvFeq = FastEqUtil[PropsVal] { (a, b) =>
    (a.flag ===* b.flag) &&
    (a.msgCode ===* b.msgCode)
  }

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Props_t]) {

    private val _onRowClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ($.state: CallbackTo[Props_t]) >>= { s =>
        val wasChecked = s.flag contains true
        s.onChange( !wasChecked )
      }
    }

    private val _onSwitchChanged = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val newChecked = e.target.checked
      ($.state: CallbackTo[Props_t]) >>= { s =>
        s.onChange( newChecked )
      }
    }

    def render(s: Props_t): VdomElement = {
      MuiListItem(
        new MuiListItemProps {
          override val button = true
          override val onClick = _onRowClickCbF
        }
      )(
        // Заголовочек:
        MuiListItemText()(
          crCtxP.message( s.msgCode ),
        ),

        {
          val chs = List[VdomNode](
            ReactCommonUtil.maybeNode( s.flag.isPending ) {
              treeStuffR.LineProgress()
            },
            MuiSwitch(
              new MuiSwitchProps {
                override val checked  = s.flag contains true
                override val disabled = s.flag.isPending
                override val onChange = _onSwitchChanged
              }
            ),
          )
          lkNodesFormCssP.consume { lknCss =>
            MuiListItemSecondaryAction( lknCss.Node.secActFlexLineProps )( chs: _* )
          }
        },
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
