package io.suggest.lk.adv.r

import diode._
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.adv.free.MAdv4Free
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.lk.adv.m.SetAdv4Free
import io.suggest.react.ReactCommonUtil.Implicits.vdomElOptionExt
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent, ReactEventFromInput}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB

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
    def onChange(e: ReactEventFromInput): Callback = {
      val v2 = e.target.checked
      dispatchOnProxyScopeCB($, SetAdv4Free(v2))
    }

    def render(props: Props, checkedOptConn: State): VdomElement = {
      props().whenDefinedEl { v =>
        <.div(
          ^.`class` := Css.Lk.Adv.Su.CONTAINER,

          <.label(
            ^.`class` := Css.CLICKABLE,

            // Единственная динамическая часть компонента: input-галочка
            checkedOptConn { checkedOptProx =>
              checkedOptProx().whenDefinedEl { checked =>
                <.input(
                  ^.`type`    := HtmlConstants.Input.checkbox,
                  ^.name      := v.static.fn,
                  ^.checked   := checked,
                  ^.onChange ==> onChange
                )
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
        )
      }
    }

  }

  val component = ScalaComponent.builder[Props]("Adv4Free")
    .initialStateFromProps { p =>
      // Коннекшен каждый раз генерит новый инстанс Option[Boolean], но это оптимизируется с помощью ValueEq O(1):
      // теперь паразитный рендер подавляется, несмотря на постоянную пересборку результата zoom-функции.
      p.connect( _.map(_.checked) )( FastEq.ValueEq )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
