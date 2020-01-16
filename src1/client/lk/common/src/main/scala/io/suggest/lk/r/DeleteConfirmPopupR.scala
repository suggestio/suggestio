package io.suggest.lk.r

import diode.react.ModelProxy
import diode.react.ReactPot.potWithReact
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.m.{DeleteConfirmPopupCancel, DeleteConfirmPopupOk, MDeleteConfirmPopupS}
import io.suggest.lk.r.popup.PopupR
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.03.17 18:01
  * Description: Компонент формы удаления узла, рендерится в попапе.
  */
object DeleteConfirmPopupR {

  type Props = ModelProxy[Option[MDeleteConfirmPopupS]]



  class Backend($: BackendScope[Props, Unit]) {

    /** Callback клика по кнопке ПОДТВЕРЖДЕНИЯ удаления узла. */
    private def onOkClick: Callback = {
      dispatchOnProxyScopeCB($, DeleteConfirmPopupOk)
    }

    /** Callback нажатия кнопки ОТМЕНЫ удаления узла. */
    private def onCancelClick: Callback = {
      dispatchOnProxyScopeCB($, DeleteConfirmPopupCancel)
    }


    def render(propsProxy: Props): VdomElement = {
      propsProxy().whenDefinedEl { props =>

        propsProxy.wrap { _ =>
          PopupR.PropsVal(
            closeable = Some(onCancelClick)
          )
        } { popPropsProxy =>
          PopupR(popPropsProxy) {
            val delPot = props.request
            // Происходит удаление узла или подготовка к этому.
            <.div(
              // Рендерить форму, когда Pot пуст.
              delPot.renderEmpty {
                <.div(

                  // Рендер вопроса "Вы уверены?".
                  <.h2(
                    ^.`class` := Css.Lk.MINOR_TITLE,
                    Messages(MsgCodes.`Are.you.sure`)
                  ),

                  <.div(
                    ^.`class` := Css.flat( Css.Buttons.BTN_W, Css.Size.M ),

                    // Кнопка подтверждения удаления, красная.
                    <.a(
                      ^.`class` := Css.flat(Css.Buttons.BTN, Css.Buttons.NEGATIVE, Css.Size.M),
                      ^.onClick --> onOkClick,
                      Messages(MsgCodes.`Yes.delete.it`)
                    ),

                    HtmlConstants.NBSP_STR,

                    // Кнопка отмены удаления:
                    <.a(
                      ^.`class` := Css.flat(Css.Buttons.BTN, Css.Buttons.MINOR, Css.Size.M, Css.Buttons.LIST),
                      ^.onClick --> onCancelClick,
                      Messages(MsgCodes.`Cancel`)
                    )
                  )

                )
              },

              // Когда идёт запрос к серверу, рендерить ожидание
              delPot.renderPending { _ =>
                <.div(
                  LkPreLoaderR.AnimMedium,
                  Messages(MsgCodes.`Please.wait`)
                )
              },

              // Ошибку удаления можно выводить на экран.
              delPot.renderFailed { ex =>
                <.div(
                  <.span(
                    ^.`class` := Css.Colors.RED,
                    ^.title := ex.toString,
                    Messages(MsgCodes.`Error`)
                  ),

                  // Кнопка закрытия ошибочной формы.
                  <.a(
                    ^.`class` := Css.flat(Css.Buttons.BTN, Css.Buttons.MINOR, Css.Size.M),
                    ^.onClick --> onCancelClick,
                    Messages(MsgCodes.`Close`)
                  )
                )
              }

            )
          }
        }(implicitly, PopupR.PopupPropsValFastEq)
      }
    }

  }


  val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
