package io.suggest.lk.tags.edit.r

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.tags.edit.MTagsEditQueryProps
import io.suggest.common.tags.search.MTagsFound
import io.suggest.css.Css
import io.suggest.lk.tags.edit.m.MTagsEditState
import io.suggest.lk.vm.LkMessagesWindow.Messages
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 22:12
  * Description: Реализация редактора тегов на базе react + diode.
  *
  * Тут всё спроектировано под M.wrap(TagsEditR(_)), без connect.
  * Внутренние же компоненты прилинкованы через .connect().
  */
object TagsEditR {

  type Props = ModelProxy[MTagsEditState]


  /** Состояние компонента содержит только immutable-инстансы коннекшенов до суб-моделей.  */
  case class State(
                    tagQueryConn  : ReactConnectProxy[MTagsEditQueryProps], // = p.connect(_.state.query)
                    tagsFoundConn : ReactConnectProxy[Pot[MTagsFound]],
                    tagsExistConn : ReactConnectProxy[Set[String]]
                  )


  /** React-компонент редактора тегов. */
  protected class Backend($: BackendScope[Props, State]) {

    /** Выполнить рендер редактора тегов. */
    def render(p: Props, s: State): ReactElement = {
      <.div(
        // Локализованный заголовок виджета
        <.h2(
          ^.`class` := Css.Lk.MINOR_TITLE,
          Messages( "Tags.choosing" )
        ),

        // поле ввода имени тега.
        s.tagQueryConn( TagNameInpR.apply ),

        // поисковый контейнер с подсказками названий тегов.
        s.tagsFoundConn( TagsFoundR.apply ),

        // tagAddBtn: Кнопка "добавить", изначально она пропада под списком подсказок
        TagAddBtnR(p),

        // tagExistsCont: Уже добавленные к заказу гео-теги.
        s.tagsExistConn( TagsExistsR.apply )

      )
    }  // def render()

  } // class Backend


  protected val component = ReactComponentB[Props]("TagsEdit")
    .initialState_P { p =>
      // Инициализировать связи до модели для нужд суб-компонентов:
      State(
        tagQueryConn  = p.connect(_.props.query),
        tagsFoundConn = p.connect(_.found),
        tagsExistConn = p.connect(_.props.tagsExists)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
