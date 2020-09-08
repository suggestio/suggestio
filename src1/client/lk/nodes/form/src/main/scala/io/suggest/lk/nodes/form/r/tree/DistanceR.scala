package io.suggest.lk.nodes.form.r.tree

import com.materialui.{MuiListItem, MuiListItemSecondaryAction, MuiListItemText}
import diode.react.ModelProxy
import io.suggest.ble.MUidBeacon
import io.suggest.geo.DistanceUtil
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.ReactDiodeUtil
import io.suggest.spa.FastEqUtil
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.{BackendScope, React, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.09.2020 17:34
  * Description: Ряд "Расстояние" до маячка.
  */
class DistanceR(
                 crCtxP               : React.Context[MCommonReactCtx],
               ) {

  type Props_t = MUidBeacon
  type Props = ModelProxy[Props_t]


  implicit val uidBeaconFeq = FastEqUtil[MUidBeacon] { (a, b) =>
    (a ===* b) ||
    (a.distanceCm ==* b.distanceCm)
  }


  class Backend( $: BackendScope[Props, Props_t] ) {

    def render(bcnSignal: Props_t): VdomElement = {
      crCtxP.consume { crCtx =>
        MuiListItem()(
          MuiListItemText()(
            crCtx.messages( MsgCodes.`Distance` ),
          ),
          MuiListItemSecondaryAction()(
            crCtx.messages {
              DistanceUtil.formatDistanceCM( bcnSignal.distanceCm )
            },
          ),
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( uidBeaconFeq ) )
    .build

}
