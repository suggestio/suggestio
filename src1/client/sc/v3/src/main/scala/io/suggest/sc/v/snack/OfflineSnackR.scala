package io.suggest.sc.v.snack

import com.materialui.{MuiButton, MuiButtonProps, MuiButtonSizes, MuiColorTypes, MuiSnackBarContent, MuiSnackBarContentProps}
import diode.react.ModelProxy
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.OnlineCheckConn
import io.suggest.sc.m.dev.MOnLineS
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.06.2020 9:58
  * Description: wrap-компонент Snack'а для отображения отстутсвия соединения.
  */
class OfflineSnackR(
                     crCtxP       : React.Context[MCommonReactCtx],
                   )
  extends ISnackComp
{

  type Props_t = MOnLineS
  type Props = ModelProxy[Props_t]


  class Backend( $: BackendScope[Props, Unit] ) {

    private val _onRetryClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, OnlineCheckConn )
    }

    def render: VdomElement = {
      val notConnectedMsg = crCtxP.message( MsgCodes.`Not.connected` )

      val retryAction = MuiButton(
        new MuiButtonProps {
          override val color = MuiColorTypes.secondary
          override val size = MuiButtonSizes.small
          override val onClick = _onRetryClickCbF
        }
      )(
        crCtxP.message( MsgCodes.`Retry` )
      )

      MuiSnackBarContent {
        new MuiSnackBarContentProps {
          override val message = notConnectedMsg.rawNode
          override val action = retryAction.rawNode
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
