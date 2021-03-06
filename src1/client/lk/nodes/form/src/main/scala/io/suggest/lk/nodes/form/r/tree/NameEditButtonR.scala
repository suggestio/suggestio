package io.suggest.lk.nodes.form.r.tree

import com.materialui.{Mui, MuiIconButton, MuiIconButtonProps, MuiToolTip, MuiToolTipProps}
import diode.react.ModelProxy
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.m.NodeEditClick
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.2020 12:51
  * Description: Кнопка редактирования узла.
  */
final class NameEditButtonR(
                             crCtxP               : React.Context[MCommonReactCtx],
                           ) {

  type Props_t = Some[Boolean]
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Props_t]) {

    private lazy val _onEditClickCbF = ReactCommonUtil.cbFun1ToJsCb { e: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB($, NodeEditClick)
    }

    def render(propsProxy: Props): VdomElement = {
      val p = propsProxy.value

      MuiToolTip(
        new MuiToolTipProps {
          override val title = crCtxP.message( MsgCodes.`Change` ).rawNode
        }
      )(
        MuiIconButton(
          new MuiIconButtonProps {
            override val onClick = _onEditClickCbF
            override val disabled = p.value
          }
        )(
          Mui.SvgIcons.Edit()(),
        )
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( FastEqUtil.AnyRefFastEq ) )
    .build

}
