package io.suggest.lk.r

import diode.react.ModelProxy
import io.suggest.lk.pop.PopupR
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement}
import japgolly.scalajs.react.vdom.prefix_<^._
import ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MsgCodes
import io.suggest.lk.m.PleaseWaitPopupCloseClick
import io.suggest.sjs.common.i18n.Messages
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import PopupR.PopupPropsValFastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.17 22:04
  * Description: Попап с просьбой подождать.
  */
object PleaseWaitPopupR {

  type Props = ModelProxy[Option[Long]]

  class Backend($: BackendScope[Props, Unit]) {

    private def closeBtnClick: Callback = {
      dispatchOnProxyScopeCB( $, PleaseWaitPopupCloseClick )
    }

    def render(proxy: Props): ReactElement = {
      for (_ <- proxy()) yield {
        proxy.wrap { _ =>
          PopupR.PropsVal(
            closeable = Some(closeBtnClick)
          )
        } { popupPropsProxy =>
          PopupR(popupPropsProxy)(
            <.br,
            <.div(
              LkPreLoaderR.AnimSmall,
              HtmlConstants.SPACE,

              Messages(MsgCodes.`Please.wait`)
            )
          )
        }
      }
    }
  }

  val component = ReactComponentB[Props]("PleaseWaitPopup")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(proxy: Props) = component(proxy)

}
