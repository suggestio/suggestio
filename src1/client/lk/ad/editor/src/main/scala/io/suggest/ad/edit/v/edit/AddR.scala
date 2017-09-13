package io.suggest.ad.edit.v.edit

import diode.react.ModelProxy
import io.suggest.ad.edit.m.{AddBtnClick, AddCancelClick, AddContentClick, AddStripClick}
import io.suggest.ad.edit.m.edit.MAddS
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.sjs.common.i18n.Messages
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.09.17 14:58
  * Description: Кнопка-форма добавления стрипа.
  */
class AddR {

  type Props = ModelProxy[Option[MAddS]]


  class Backend($: BackendScope[Props, Unit]) {

    /** Клик по кнопке добавления чего-то первого шага. */
    private def onAddClick: Callback = {
      dispatchOnProxyScopeCB($, AddBtnClick)
    }

    private def onContentClick: Callback = {
      dispatchOnProxyScopeCB($, AddContentClick)
    }

    private def onStripClick: Callback = {
      dispatchOnProxyScopeCB($, AddStripClick)
    }

    private def onCancelClick: Callback = {
      dispatchOnProxyScopeCB($, AddCancelClick)
    }


    def render(p: Props): VdomElement = {
      val B = Css.Buttons
      <.div(

        p.value.fold[TagMod] {
          <.a(
            ^.`class` := Css.flat(B.BTN, B.MINOR, Css.Size.M),
            ^.onClick --> onAddClick,
            Messages( MsgCodes.`Add` ) + HtmlConstants.ELLIPSIS
          )
        } { _ =>
          <.div(
            Messages( MsgCodes.`What.to.add` ),

            <.a(
              ^.`class` := Css.flat(B.BTN, B.MAJOR, Css.Size.M),
              ^.onClick --> onContentClick,
              Messages( MsgCodes.`Content` )
            ),

            HtmlConstants.SPACE,

            <.a(
              ^.`class` := Css.flat(B.BTN, B.MINOR, Css.Size.M),
              ^.onClick --> onStripClick,
              Messages( MsgCodes.`Block` )
            ),

            HtmlConstants.SPACE,
            <.a(
              ^.`class` := Css.flat( B.CLOSE, Css.Floatt.RIGHT, Css.Display.INLINE_BLOCK ),
              ^.title := Messages( MsgCodes.`Cancel` ),
              ^.onClick --> onCancelClick
            )

          )
        }
      )
    }

  }


  val component = ScalaComponent.builder[Props]("Add")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(addSProxy: Props) = component(addSProxy)

}
