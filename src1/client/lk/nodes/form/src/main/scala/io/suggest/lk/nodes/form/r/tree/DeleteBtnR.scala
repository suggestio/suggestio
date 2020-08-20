package io.suggest.lk.nodes.form.r.tree

import com.materialui.{Mui, MuiButton, MuiButtonProps, MuiButtonVariants, MuiColorTypes, MuiIconButton, MuiIconButtonProps, MuiToolTip, MuiToolTipProps}
import diode.react.ModelProxy
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.m.MDeleteConfirmPopupS
import io.suggest.lk.nodes.form.m.NodeDeleteClick
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React.Node
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.2020 18:08
  * Description: Кнопка удаления узла.
  */
class DeleteBtnR(
                      crCtxP               : React.Context[MCommonReactCtx],
                    ) {

  type Props_t = Option[MDeleteConfirmPopupS]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Props_t]) {

    private lazy val _onDeleteClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB($, NodeDeleteClick)
    }

    def render(propsProxy: Props): VdomElement = {
      val p = propsProxy.value

      MuiToolTip(
        new MuiToolTipProps {
          override val title = crCtxP.message( MsgCodes.`Delete` ).rawNode
        }
      )(
        MuiIconButton(
          new MuiIconButtonProps {
            override val color = MuiColorTypes.secondary
            override val onClick = _onDeleteClickCbF
            override val disabled = p.nonEmpty
          }
        )(
          Mui.SvgIcons.Delete()(),
        )
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( OptFastEq.IsEmptyEq ) )
    .build

}
