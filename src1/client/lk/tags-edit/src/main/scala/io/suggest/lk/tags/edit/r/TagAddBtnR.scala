package io.suggest.lk.tags.edit.r

import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.tags.edit.m.AddCurrentTag
import io.suggest.msg.Messages
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 20:32
  * Description: Пока-статическая кнопка добавления введенного тега с очень толстым handler'ом.
  */
object TagAddBtnR {

  type Props = ModelProxy[_]

  protected class Backend($: BackendScope[Props, Unit]) {

    def onAddBtnClick: Callback = {
      dispatchOnProxyScopeCB( $, AddCurrentTag )
    }

    def render(p: Props) = {
      <.input(
        ^.`class`  := Css.flat(Css.Buttons.BTN, Css.Size.M, Css.Buttons.MAJOR),
        ^.value    := Messages( MsgCodes.`Add` ),
        ^.`type`   := HtmlConstants.Input.submit,
        ^.onClick --> onAddBtnClick
      )
    }

  }

  val component = ScalaComponent.builder[Props]("TagAddBtn")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(proxy: Props) = component(proxy)

}


