package io.suggest.lk.nodes.form.r.tree

import com.materialui.{Mui, MuiColorTypes, MuiIconButton, MuiIconButtonProps, MuiLink, MuiLinkProps, MuiToolTip, MuiToolTipProps}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.routes.routes
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.08.2020 14:47
  * Description: Компонент ссылки на ЛК узла.
  */
final class GoToLkLinkR(
                         crCtxP               : React.Context[MCommonReactCtx],
                       ) {

  type Props = String

  class Backend($: BackendScope[Props, Unit]) {

    private lazy val _onLinkClick = ReactCommonUtil.cbFun1ToJsCb { e: ReactEvent =>
      e.stopPropagationCB
    }

    def render(nodeId: Props): VdomElement = {
      MuiToolTip(
        new MuiToolTipProps {
          override val title = crCtxP.message( MsgCodes.`Go.into` ).rawNode
        }
      )(
        MuiLink(
          new MuiLinkProps {
            val href = routes.controllers.LkAds.adsPage( nodeId ).url
            override val onClick = _onLinkClick
          }
        )(
          MuiIconButton(
            new MuiIconButtonProps {
              override val color = MuiColorTypes.primary
            }
          )(
            Mui.SvgIcons.ArrowForward()(),
          )
        )
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.propsFastEqShouldComponentUpdate( FastEqUtil.AnyRefFastEq ) )
    .build

}
