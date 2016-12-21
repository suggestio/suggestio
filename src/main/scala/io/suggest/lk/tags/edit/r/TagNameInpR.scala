package io.suggest.lk.tags.edit.r

import diode.react.ModelProxy
import io.suggest.common.tags.edit.{MTagsSearchS, TagsEditConstants}
import io.suggest.css.Css
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactEventI}
import io.suggest.lk.vm.LkMessagesWindow.Messages
import io.suggest.sjs.common.spa.DAction
import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.common.html.HtmlConstants.SPACE

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 18:41
  * Description: Компонент инпута названия тега для поиска/создания оного.
  */
object TagNameInpR {

  type Props = ModelProxy[MTagsSearchS]

  protected class Backend($: BackendScope[Props, Unit]) {

    /**
      * Коллбэк ввода текста в поле имени нового тега.
      * Надо обновить состояние и запустить поисковый запрос, если требуется.
      */
    def onQueryChange(e: ReactEventI): Callback = {
      val queryStr2 = e.target.value
      $.props >>= { p =>
        p.dispatchCB( SetTagSearchQuery(queryStr2) )
      }
    }


    def render(p: Props) = {
      // tagsAddForm: поле ввода имени тега.
      <.div(
        ^.`class` := Css.Input.INPUT_W,

        // Лэйбл для ввода названия тега
        <.label(
          ^.`class` := Css.Lk.LK_FIELD_NAME,
          Messages( "Add.tags" ),

          // Рендер инпута, содержащего искомое имя тега
          <.div(
            ^.classSet1(
              Css.Input.INPUT + SPACE + Css.CLEARFIX,
              Css.Input.ERROR -> p().errors.nonEmpty
            ),
            <.div(
              ^.`class` := (Css.Input.INPUT_SHADOW + SPACE + Css.Input.JS_INPUT_W),
              <.input(
                ^.`type`   := "text",
                ^.name     := TagsEditConstants.ADD_TAGS_FN,
                ^.value    := p().text,
                ^.onChange ==> onQueryChange
              )
            )
          )
        )
      )
    }

  }


  val component = ReactComponentB[Props]("TagNameInp")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(queryState: Props) = component(queryState)

}


/** Экшен обновления имени тега. */
case class SetTagSearchQuery(query: String) extends DAction
