package io.suggest.lk.nodes.form.r.pop

import diode.react.ModelProxy
import diode.react.ReactPot.potWithReact
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.nodes.form.m.{MDeleteNodeS, NodeDeleteCancelClick, NodeDeleteOkClick}
import io.suggest.lk.pop.PopupR
import io.suggest.lk.r.LkPreLoaderR
import io.suggest.sjs.common.i18n.Messages
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.03.17 18:01
  * Description: Компонент формы удаления узла, рендерится в попапе.
  */
object DeleteNodeR {

  type Props = ModelProxy[Option[MDeleteNodeS]]


  class Backend($: BackendScope[Props, Unit]) {

    /** Callback клика по кнопке ПОДТВЕРЖДЕНИЯ удаления узла. */
    private def onOkClick: Callback = {
      dispatchOnProxyScopeCB($, NodeDeleteOkClick )
    }

    /** Callback нажатия кнопки ОТМЕНЫ удаления узла. */
    private def onCancelClick: Callback = {
      dispatchOnProxyScopeCB($, NodeDeleteCancelClick )
    }


    def render(propsProxy: Props): ReactElement = {
      for (props <- propsProxy()) yield {

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

                    // Кнопки поменяны местами для защиты от двойных нажатий.
                    // Кнопка отмены удаления:
                    <.a(
                      ^.`class` := Css.flat(Css.Buttons.BTN, Css.Buttons.MINOR, Css.Size.M),
                      ^.onClick --> onCancelClick,
                      Messages(MsgCodes.`Cancel`)
                    ),

                    HtmlConstants.NBSP_STR,

                    // Кнопка подтверждения удаления, красная.
                    <.a(
                      ^.`class` := Css.flat(Css.Buttons.BTN, Css.Buttons.NEGATIVE, Css.Size.M, Css.Buttons.LIST),
                      ^.onClick --> onOkClick,
                      Messages(MsgCodes.`Yes.delete.it`)
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
          }: ReactElement
        }
      }
    }

  }


  val component = ReactComponentB[Props]("DeleteNode")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}