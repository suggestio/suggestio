package io.suggest.lk.tags.edit.r

import diode.FastEq
import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.tags.edit.{MTagsEditProps, MTagsEditQueryProps}
import io.suggest.common.tags.search.MTagsFound
import io.suggest.css.Css
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

  type Props = ModelProxy[PropsVal]


  /** Пропертисы компонента -- отзумленный контейнер некоторых полей root-модели. */
  case class PropsVal(
                      tagsFound  : Pot[MTagsFound],
                      state      : MTagsEditProps
                     )

  implicit object PropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.tagsFound eq b.tagsFound) &&
        (a.state eq b.state)
    }
  }


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
        tagQueryConn  = p.connect(_.state.query),
        tagsFoundConn = p.connect(_.tagsFound),
        tagsExistConn = p.connect(_.state.tagsExists)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
