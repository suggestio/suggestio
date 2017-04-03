package io.suggest.lk.pop

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.r.ReactDiodeUtil
import io.suggest.sjs.common.i18n.Messages
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
                       closeable  : Option[Callback]  = None,
                       hSize      : String            = Css.Size.M,
                       css        : List[String]      = Nil,
                       topPc      : Int               = 32
                     )

  implicit object PopupPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.closeable eq b.closeable) &&
        (a.hSize eq b.hSize) &&
        (a.css eq b.css) &&
        (a.topPc == b.topPc)
    }
  }


  class Backend($: BackendScope[Props, Unit]) {

    def render(propsProxy: Props, pc: PropsChildren): ReactElement = {
      val p = propsProxy()
      <.div(
        ^.`class`  := Css.flat1( Css.Lk.Popup.POPUP :: p.hSize :: p.css ),
        ^.top      := p.topPc.pct,
        ^.onClick ==> ReactDiodeUtil.eStopPropagationCB,

        for (closeCB <- p.closeable) yield {
          <.div(
            ^.`class` := Css.Lk.Popup.POPUP_HEADER,

            <.a(
              ^.`class`  := Css.flat(Css.Lk.Popup.CLOSE, Css.Floatt.RIGHT),
              //^.href     := dom.window.location.href,
              ^.title    := Messages( MsgCodes.`Close` ),
              ^.onClick --> closeCB
            )
          )
        },

        // Наконец, рендер содержимого попапа:
        pc
      )
    }

  }


  val component = ReactComponentB[Props]("Popup")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props)(children: ReactNode*) = component(props, children: _*)

}
