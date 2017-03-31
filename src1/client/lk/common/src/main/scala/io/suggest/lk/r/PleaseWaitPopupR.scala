package io.suggest.lk.r

import diode.react.ModelProxy
import io.suggest.lk.pop.PopupR
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement}
import ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MsgCodes
import io.suggest.lk.m.PleaseWaitPopupCloseClick
import io.suggest.sjs.common.i18n.Messages

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.17 22:04
  * Description: Попап с просьбой подождать.
  */
object PleaseWaitPopupR {

  type Props = ModelProxy[_]

  class Backend($: BackendScope[Props, Unit]) {

    private def closeBtnClick: Callback = {
      dispatchOnProxyScopeCB(
        $.asInstanceOf[BackendScope[ModelProxy[AnyRef], Unit]],
        PleaseWaitPopupCloseClick
      )
    }

    def render(proxy: Props): ReactElement = {
      proxy.wrap { _ =>
        PopupR.PropsVal(
          closeable = Some(closeBtnClick)
        )
      } { popupPropsProxy =>
        PopupR(popupPropsProxy)(
          LkPreLoaderR.AnimMedium,
          HtmlConstants.SPACE,

          Messages( MsgCodes.`Please.wait` ),
          HtmlConstants.ELLIPSIS
        )
      }
    }
  }

  val component = ReactComponentB[Props]("PleaseWaitPopup")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(proxy: Props) = component(proxy)

}
