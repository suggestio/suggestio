package io.suggest.lk.nodes.form.r.tree

import com.materialui.{MuiListItem, MuiListItemText, MuiListItemTextProps}
import diode.react.ModelProxy
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.m.MNodeBeaconState
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
class DistanceRowR(
                    distanceValueR       : DistanceValueR,
                    crCtxP               : React.Context[MCommonReactCtx],
                  ) {

  type Props_t = MNodeBeaconState
  type Props = ModelProxy[Props_t]


  class Backend( $: BackendScope[Props, Props_t] ) {

    def render(propsProxy: Props, beaconState: Props_t): VdomElement = {
      crCtxP.consume { crCtx =>
        MuiListItem()(

          // Текст слева
          MuiListItemText(
            new MuiListItemTextProps {
              override val primary = crCtx.messages( MsgCodes.`Distance` )
              // TODO secondary: Тут выводится - сколько секунд назад измерено, либо выводится сообщение, что "Вне области видимости".
            }
          )(),

          // Расстояние или бесконечность - справа
          distanceValueR.component(propsProxy),

        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( FastEqUtil.AnyValueEq ) )
    .build

}
