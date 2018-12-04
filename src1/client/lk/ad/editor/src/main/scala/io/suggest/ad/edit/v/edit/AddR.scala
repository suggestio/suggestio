package io.suggest.ad.edit.v.edit

import diode.react.ModelProxy
import io.suggest.ad.edit.m.{AddContentClick, AddStripClick}
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.09.17 14:58
  * Description: Кнопка-форма добавления стрипа.
  */
class AddR {

  type Props = ModelProxy[_]


  class Backend($: BackendScope[Props, Unit]) {

    private def onContentClick: Callback = {
      dispatchOnProxyScopeCB($, AddContentClick)
    }

    private def onStripClick: Callback = {
      dispatchOnProxyScopeCB($, AddStripClick)
    }


    def render(p: Props): VdomElement = {
      val B = Css.Buttons
      <.div(

        <.div(
          Messages( MsgCodes.`What.to.add` ),

          <.br,
          <.br,

          <.a(
            ^.`class` := Css.flat(B.BTN, B.MAJOR, Css.Size.M),
            ^.onClick --> onContentClick,
            Messages( MsgCodes.`Content` )
          ),

          <.br,
          <.br,

          <.a(
            ^.`class` := Css.flat(B.BTN, B.MINOR, Css.Size.M),
            ^.onClick --> onStripClick,
            Messages( MsgCodes.`Block` )
          )

        )
      )
    }

  }


  val component = ScalaComponent.builder[Props](getClass.getSimpleName)
    .stateless
    .renderBackend[Backend]
    .build

  def apply(addSProxy: Props) = component(addSProxy)

}
