package io.suggest.lk.adv.r

import io.suggest.css.Css
import io.suggest.lk.adv.m.IAdv4FreeProps
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactEventI}
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.16 18:17
  * Description: Реализация reusable-компонента поддержки бесплатного размещения для суперюзеров.
  */
object Adv4FreeR {

  /**
    * Пропертисы, приходящие свыше.
    * @param onChange Что дёргать после наступления изменений?
    * @param config Конфигурация компонента, отрендеренная с сервера.
    */
  case class Props(
    onChange  : () => Unit,
    config    : IAdv4FreeProps
  )

  /**
    * Состояние компонента галочки бесплатного размещения.
    *
    * @param value Текущее значение галочки. true по дефолту, т.к. этот компонент рендерится только для админов.
    */
  case class State(
    value   : Boolean  = true
  )


  protected class Backend($: BackendScope[Props, State]) {

    /** Реакция на изменение состояния галочки. */
    def onChange(e: ReactEventI): Callback = {
      val v2 = e.target.checked
      val sCb = $.modState { s0 =>
        s0.copy(
          value = v2
        )
      }
      sCb >> $.props |> { props =>
        props.onChange()
      }
    }

    def render(props: Props, state: State) = {
      <.div(
        ^.`class` := Css.Lk.Adv.Su.CONTAINER,

        <.label(
          <.input(
            ^.`type`    := "checkbox",
            ^.name      := props.config.fn,
            ^.checked   := state.value,
            ^.onChange  ==> onChange
          ),
          <.span(
            ^.`class` := Css.Input.STYLED_CHECKBOX
          ),
          <.span(
            ^.`class` := (Css.Input.CHECKBOX_TITLE :: Css.Buttons.MAJOR :: Nil).mkString(" "),
            props.config.title
          )
        )
      )
    }

  }

  val component = ReactComponentB[Props]("Adv4Free")
    .initialState( State() )
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
