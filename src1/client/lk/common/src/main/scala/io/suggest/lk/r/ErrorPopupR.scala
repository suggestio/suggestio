package io.suggest.lk.r

import diode.react.ModelProxy
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.m.{ErrorPopupCloseClick, MErrorPopupS}
import io.suggest.lk.pop.PopupR
import PopupR.PopupPropsValFastEq
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.17 22:22
  * Description: Попап ошибки.
  */
object ErrorPopupR {

  type Props_t = Option[MErrorPopupS]
  type Props = ModelProxy[Props_t]

  protected class Backend($: BackendScope[Props, Unit]) {

    private def closeBtnClick: Callback = {
      dispatchOnProxyScopeCB( $, ErrorPopupCloseClick )
    }

    def render(proxy: Props): VdomElement = {
      proxy().whenDefinedEl { popupS =>
        proxy.wrap { _ =>
          PopupR.PropsVal(
            closeable = Some(closeBtnClick)
          )
        } { popupPropsProxy =>
          PopupR(popupPropsProxy)(
            <.div(
              <.h2(
                ^.`class` := Css.Lk.MINOR_TITLE,
                Messages(MsgCodes.`Something.gone.wrong`)
              ),

              popupS.messages.headOption.whenDefined { _ =>
                <.div(
                  <.ul(
                    popupS.messages.iterator.zipWithIndex.toVdomArray { case (msg, i) =>
                      <.li(
                        ^.key := i.toString,
                        Messages( msg )
                      )
                    }
                  ),
                  <.br
                )
              },

              popupS.exception.whenDefined { ex =>
                <.div(
                  <.div(
                    ^.`class` := Css.Colors.RED,
                    Messages( MsgCodes.`Error` )
                  ),

                  <.div(
                    ex.toString()
                  ),
                  <.br,

                  Messages( MsgCodes.`Please.try.again.later` )
                )
              },

              <.br,

              <.a(
                ^.`class` := Css.flat( Css.Buttons.BTN, Css.Buttons.BTN_W, Css.Buttons.MINOR, Css.Size.M ),
                ^.onClick --> closeBtnClick,
                Messages( MsgCodes.`Close` )
              ),
              <.br
            )
          )
        }
      }
    }

  }


  val component = ScalaComponent.builder[Props](getClass.getSimpleName)
    .stateless
    .renderBackend[Backend]
    .build

  def apply(exOptProxy: Props) = component( exOptProxy )

}
