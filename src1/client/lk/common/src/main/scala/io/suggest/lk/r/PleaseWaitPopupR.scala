package io.suggest.lk.r

import diode.react.ModelProxy
import io.suggest.lk.pop.PopupR
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MsgCodes
import io.suggest.lk.m.PleaseWaitPopupCloseClick
import io.suggest.react.ReactCommonUtil.Implicits._
import PopupR.PopupPropsValFastEq
import io.suggest.msg.Messages

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

    def render(proxy: Props): VdomElement = {
      proxy().whenDefinedEl { _ =>
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

  val component = ScalaComponent.builder[Props]("PleaseWaitPopup")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(proxy: Props) = component(proxy)

}
