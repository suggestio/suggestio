package io.suggest.lk.tags.edit.r

import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.tags.edit.m.AddCurrentTag
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB}
import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.sjs.common.i18n.Messages

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 20:32
  * Description: Пока-статическая кнопка добавления введенного тега с очень толстым handler'ом.
  */
object TagAddBtnR {

  type Props = ModelProxy[_]

  protected class Backend($: BackendScope[Props, _]) {

    def onAddBtnClick: Callback = {
      $.props >>= { p =>
        p.dispatchCB(AddCurrentTag)
      }
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

  val component = ReactComponentB[Props]("TagAddBtn")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(proxy: Props) = component(proxy)

}


