package io.suggest.lk.tags.edit.r

import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.common.tags.edit.{MTagsEditQueryProps, TagsEditConstants}
import io.suggest.css.Css
import japgolly.scalajs.react.{BackendScope, Callback, ReactEventFromInput, ReactKeyboardEventFromInput, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.common.html.HtmlConstants.SPACE
import io.suggest.i18n.MsgCodes
import io.suggest.lk.tags.edit.m.{AddCurrentTag, SetTagSearchQuery}
import io.suggest.msg.Messages
import org.scalajs.dom.ext.KeyCode
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 18:41
  * Description: Компонент инпута названия тега для поиска/создания оного.
  */
object TagNameInpR {

  type Props = ModelProxy[MTagsEditQueryProps]

  protected class Backend($: BackendScope[Props, Unit]) {

    /**
      * Коллбэк ввода текста в поле имени нового тега.
      * Надо обновить состояние и запустить поисковый запрос, если требуется.
      */
    def onQueryChange(e: ReactEventFromInput): Callback = {
      val queryStr2 = e.target.value
      dispatchOnProxyScopeCB( $, SetTagSearchQuery(queryStr2) )
    }

    /** Коллбек для реакции на нажатие некоторых особых клавиш на клавиатуре во время ввода. */
    def onKeyUp(e: ReactKeyboardEventFromInput): Callback = {
      if (e.keyCode == KeyCode.Enter) {
        dispatchOnProxyScopeCB($, AddCurrentTag)
      } else {
        Callback.empty
      }
    }


    def render(p: Props) = {
      // tagsAddForm: поле ввода имени тега.
      <.div(
        ^.`class` := Css.Input.INPUT_W,

        // Лэйбл для ввода названия тега
        <.label(
          ^.`class` := Css.Lk.LK_FIELD_NAME,
          Messages( MsgCodes.`Add.tags` ),

          // Рендер инпута, содержащего искомое имя тега
          <.div(
            ^.classSet1(
              Css.Input.INPUT + SPACE + Css.CLEARFIX,
              Css.Input.ERROR -> p().errors.nonEmpty
            ),
            <.div(
              ^.`class` := (Css.Input.INPUT_SHADOW + SPACE + Css.Input.JS_INPUT_W),
              <.input(
                ^.`type`   := HtmlConstants.Input.text,
                ^.name     := TagsEditConstants.ADD_TAGS_FN,
                ^.value    := p().text,
                ^.onChange ==> onQueryChange,
                ^.onKeyUp  ==> onKeyUp
              )
            )
          )
        )
      )
    }

  }


  val component = ScalaComponent.builder[Props]("TagNameInp")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(queryState: Props) = component(queryState)

}



