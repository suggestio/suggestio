package io.suggest.lk.nodes.form.r.tree

import com.materialui.MuiListItemSecondaryAction
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.geo.DistanceUtil
import io.suggest.i18n.MCommonReactCtx
import io.suggest.lk.nodes.form.m.MNodeBeaconState
import io.suggest.react.ReactDiodeUtil
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Builder.defaultToNoBackend
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.09.2020 21:22
  * Description: Компонент расстояния до маячка в правой колонке.
  * Рендерится сразу в нескольких местах даже для одного элемента, поэтому вынесен сюда.
  */
class DistanceValueR(
                      crCtxP               : React.Context[MCommonReactCtx],
                    ) {

  type Props_t = MNodeBeaconState
  type Props = ModelProxy[Props_t]


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .render_S { beaconState =>
      MuiListItemSecondaryAction()(

        beaconState.data
          .lastDistanceCm
          .filter(_ => beaconState.isVisible)
          .fold[VdomNode] {
            // Нет расстояния - знак бесконечности.
            HtmlConstants.INFINITY

          } { distanceCm =>
            val distanceMsg = DistanceUtil.formatDistanceCM( distanceCm )
            crCtxP.consume { crCtx =>
              crCtx.messages( distanceMsg )
            }
          },

      )
    }
    .configure(
      ReactDiodeUtil.statePropsValShouldComponentUpdate(
        FastEqUtil { (a, b) =>
          (a.isVisible ==* b.isVisible) &&
          (a.data.lastDistanceCm ==* b.data.lastDistanceCm)
        }
      )
    )
    .build

}
