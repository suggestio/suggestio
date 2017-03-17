package io.suggest.lk.pop

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.sjs.common.i18n.Messages
import io.suggest.sjs.common.spa.DAction
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 18:10
  * Description: React-компонент для попапов личного кабинета поверх экрана.
  */
object PopupR {

  type Props = ModelProxy[PropsVal]

  case class PropsVal(
                       closeable  : Option[DAction] = None,
                       hSize      : String          = Css.Size.M,
                       css        : List[String]    = Nil
                     )

  implicit object PopupPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.closeable eq b.closeable) &&
        (a.hSize eq b.hSize) &&
        (a.css eq b.css)
    }
  }


  class Backend($: BackendScope[Props, Unit]) {

    def onClose: Callback = {
      $.props >>= { p =>
        p.dispatchCB( p().closeable.get )
      }
    }

    def render(propsProxy: Props, pc: PropsChildren): ReactElement = {
      val p = propsProxy()
      <.div(
        ^.`class` := Css.flat1( Css.Lk.Popup.POPUP :: p.hSize :: p.css ),

        <.div(
          ^.`class` := Css.Lk.Popup.POPUP_HEADER,

          for (closeMsg <- p.closeable) yield {
            <.a(
              ^.`class`  := Css.flat(Css.Lk.Popup.CLOSE, Css.Floatt.RIGHT),
              ^.href     := dom.window.location.href,
              ^.title    := Messages("Close"),
              ^.onClick --> onClose
            )
          },

          // Наконец, рендер содержимого попапа:
          pc
        )
      )
    }

  }


  val component = ReactComponentB[Props]("Popup")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props)(children: ReactNode*) = component(props, children: _*)

}
