package io.suggest.lk.r

import diode.react.ModelProxy
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.m.ErrorPopupCloseClick
import io.suggest.lk.pop.PopupR
import io.suggest.sjs.common.i18n.Messages
import PopupR.PopupPropsValFastEq
import io.suggest.react.ReactCommonUtil.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.17 22:22
  * Description: Попап ошибки.
  */
object ErrorPopupR {

  type Props = ModelProxy[Option[Throwable]]

  protected class Backend($: BackendScope[Props, Unit]) {

    private def closeBtnClick: Callback = {
      dispatchOnProxyScopeCB( $, ErrorPopupCloseClick )
    }

    def render(proxy: Props): VdomElement = {
      proxy().whenDefinedEl { ex =>
        proxy.wrap { _ =>
          PopupR.PropsVal(
            closeable = Some(closeBtnClick)
          )
        } { popupPropsProxy =>
          PopupR(popupPropsProxy)(
            <.h2(
              ^.`class` := Css.Lk.MINOR_TITLE,
              Messages( MsgCodes.`Something.gone.wrong` )
            ),

            <.div(
              ^.`class` := Css.Colors.RED,
              Messages( MsgCodes.`Error` )
            ),

            <.div(
              ex.toString()
            ),
            <.br,

            Messages( MsgCodes.`Please.try.again.later` ),
            <.br,

            <.a(
              ^.`class` := Css.flat( Css.Buttons.BTN, Css.Buttons.BTN_W, Css.Buttons.NEGATIVE, Css.Size.M ),
              ^.onClick --> closeBtnClick,
              Messages( MsgCodes.`Close` )
            ),
            <.br
          )
        }
      }
    }

  }


  val component = ScalaComponent.builder[Props]("ErrorPopup")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(exOptProxy: Props) = component( exOptProxy )

}
