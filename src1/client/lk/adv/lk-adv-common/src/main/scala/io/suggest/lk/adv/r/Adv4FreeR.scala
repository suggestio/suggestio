package io.suggest.lk.adv.r

import diode.{ActionHandler, ActionResult, ModelRW}
import diode.react.ModelProxy
import io.suggest.adv.geo.MAdv4FreeS
import io.suggest.css.Css
import io.suggest.sjs.common.spa.DAction
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement, ReactEventI}
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.16 18:17
  * Description: Реализация reusable-компонента поддержки бесплатного размещения для суперюзеров.
  */
object Adv4FreeR {

  type Props = ModelProxy[MAdv4FreeS]


  protected class Backend($: BackendScope[Props, Unit]) {

    /** Реакция на изменение состояния галочки. */
    def onChange(e: ReactEventI): Callback = {
      val v2 = e.target.checked
      $.props >>= { props =>
        props.dispatchCB(Adv4FreeChanged(v2))
      }
    }

    def render(props: Props): ReactElement = {
      val v = props()
      <.div(
        ^.`class` := Css.Lk.Adv.Su.CONTAINER,

        <.label(
          <.input(
            ^.`type`    := "checkbox",
            ^.name      := v.static.fn,
            ^.checked   := v.checked,
            ^.onChange ==> onChange
          ),
          <.span(
            ^.`class` := Css.Input.STYLED_CHECKBOX
          ),
          <.span(
            ^.`class` := (Css.Input.CHECKBOX_TITLE :: Css.Buttons.MAJOR :: Nil).mkString(" "),
            v.static.title
          )
        )
      )
    }

  }

  val component = ReactComponentB[Props]("Adv4Free")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}


/** Сигнал-экшен для diode-системы об изменении состояния галочки su-бесплатного размещения. */
case class Adv4FreeChanged(checked: Boolean) extends DAction


/**
  * Diode Action handler для реакции на галочку бесплатного размещения для суперюзеров.
  * Зуммировать доступ желательно прямо до поля галочки.
  */
class Adv4FreeActionHandler[M](modelRW: ModelRW[M, Option[Boolean]]) extends ActionHandler(modelRW) {
  override protected def handle: PartialFunction[Any, ActionResult[M]] = {
    case e: Adv4FreeChanged =>
      val checked0 = value.contains(true)
      val checked2 = e.checked
      if (checked0 != checked2) {
        updated( Some(checked2) )
      } else {
        noChange
      }
  }
}
