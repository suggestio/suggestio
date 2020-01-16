package io.suggest.lk.r.popup

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.ueq.ReactUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

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
                       topPc      : Int               = 32,
                       tagMod     : Option[TagMod]    = None
                     )

  implicit object PopupPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.closeable eq b.closeable) &&
        (a.hSize ===* b.hSize) &&
        (a.css ===* b.css) &&
        (a.topPc ==* b.topPc) &&
        (a.tagMod ===* b.tagMod)
    }
  }


  class Backend($: BackendScope[Props, Unit]) {

    def render(propsProxy: Props, pc: PropsChildren): VdomElement = {
      val p = propsProxy()
      <.div(
        ^.`class`  := Css.flat1( Css.Lk.Popup.POPUP :: p.hSize :: p.css ),
        ^.top      := p.topPc.pct,
        p.tagMod.whenDefined,

        ^.onClick ==> ReactCommonUtil.stopPropagationCB,

        p.closeable.whenDefined { closeCB =>
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


  val component = ScalaComponent.builder[Props]("Popup")
    .stateless
    .renderBackendWithChildren[Backend]
    .build

  def apply(props: Props)(children: VdomNode*) = component(props)(children: _*)

}
