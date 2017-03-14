package io.suggest.lk.adv.r

import diode._
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.adv.free.MAdv4Free
import io.suggest.css.Css
import io.suggest.lk.adv.m.SetAdv4Free
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement, ReactEventI}
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.16 18:17
  * Description: Реализация reusable-компонента поддержки бесплатного размещения для суперюзеров.
  *
  * Реализована для использования с помощью react-diode M.wrap().
  */
object Adv4FreeR {

  type Props = ModelProxy[Option[MAdv4Free]]

  /** Состояние является просто коннекшеном до значения галочки в опциональных пропертисах. */
  protected[this] type State = /*checkedConn: */ ReactConnectProxy[Option[Boolean]]

  protected class Backend($: BackendScope[Props, State]) {

    /** Реакция на изменение состояния галочки. */
    def onChange(e: ReactEventI): Callback = {
      val v2 = e.target.checked
      $.props >>= { props =>
        props.dispatchCB(SetAdv4Free(v2))
      }
    }

    def render(props: Props, checkedOptConn: State): ReactElement = {
      for (v <- props()) yield {
        <.div(
          ^.`class` := Css.Lk.Adv.Su.CONTAINER,

          <.label(
            // Единственная динамическая часть компонента: input-галочка
            checkedOptConn { checkedOptProx =>
              for (checked <- checkedOptProx()) yield {
                <.input(
                  ^.`type`    := "checkbox",
                  ^.name      := v.static.fn,
                  ^.checked   := checked,
                  ^.onChange ==> onChange
                ): ReactElement
              }
            },
            <.span(
              ^.`class` := Css.Input.STYLED_CHECKBOX
            ),
            <.span(
              ^.`class` := Css.flat(Css.Input.CHECKBOX_TITLE, Css.Buttons.MAJOR),
              v.static.title
            )
          )
        ): ReactElement
      }
    }

  }

  val component = ReactComponentB[Props]("Adv4Free")
    .initialState_P { p =>
      // Коннекшен каждый раз генерит новый инстанс Option[Boolean], но это оптимизируется с помощью ValueEq O(1):
      // теперь паразитный рендер подавляется, несмотря на постоянную пересборку результата zoom-функции.
      p.connect( _.map(_.checked) )( FastEq.ValueEq )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
