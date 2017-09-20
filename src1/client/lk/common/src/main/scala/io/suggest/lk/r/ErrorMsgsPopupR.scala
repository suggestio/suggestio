package io.suggest.lk.r

import diode.react.ModelProxy
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.css.Css
import io.suggest.i18n.{IMessage, MsgCodes}
import io.suggest.lk.m.ErrorPopupCloseClick
import io.suggest.lk.pop.PopupR
import io.suggest.sjs.common.i18n.Messages
import PopupR.PopupPropsValFastEq
import io.suggest.react.ReactCommonUtil.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.17 16:41
  * Description: React-компонент попапа со списком сообщений о каких-то проблемах.
  */
object ErrorMsgsPopupR {

  type Props = ModelProxy[Seq[IMessage]]


  protected class Backend($: BackendScope[Props, Unit]) {

    private def closeBtnClick: Callback = {
      dispatchOnProxyScopeCB( $, ErrorPopupCloseClick )
    }

    def render(proxy: Props): VdomElement = {
      val msgs = proxy.value
      msgs.headOption.whenDefinedEl { _ =>
        proxy.wrap { _ =>
          PopupR.PropsVal(
            closeable = Some(closeBtnClick)
          )
        } { popupPropsProxy =>
          PopupR(popupPropsProxy)(
            <.h2(
              ^.`class` := Css.Lk.MINOR_TITLE,
              Messages( MsgCodes.`Error` )
            ),

            <.div(
              <.ul(
                msgs.iterator.zipWithIndex.toVdomArray { case (msg, i) =>
                  <.li(
                    ^.key := i.toString,
                    Messages( msg )
                  )
                }
              )
            ),
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


  val component = ScalaComponent.builder[Props]("EMsgsPop")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(exOptProxy: Props) = component( exOptProxy )

}
