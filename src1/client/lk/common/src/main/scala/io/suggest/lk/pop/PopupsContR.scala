package io.suggest.lk.pop

import diode.UseValueEq
import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.doc.DocumentVm
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom.raw.HTMLDivElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 18:15
  * Description: React-компонент контейнера react-попапов.
  */
object PopupsContR {

  type Props = ModelProxy[PropsVal]

  case class PropsVal(
                       visible: Boolean
                     )
    extends UseValueEq

  /*implicit object PopContPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      a == b
    }
  }*/


  class Backend($: BackendScope[Props, Unit]) {

    def render(propsProxy: Props, pc: PropsChildren): ReactElement = {
      val p = propsProxy()
      <.aside(
        ^.classSet1(
          Css.flat( Css.Lk.Popup.POPUPS, Css.Lk.Popup.POPUPS_CONTAINER ),
          Css.Display.VISIBLE -> p.visible
        ),

        // Отрендерить все попапы:
        pc
      )
    }

  }


  val component = ReactComponentB[Props]("PopCont")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props)(children: ReactNode*) = component(props, children: _*)

  /**
    * Надо куда-то рендерить контейнер попапов, прежде чем пользоваться им.
    * Тут у нас инициализация этого счастья: добавление div'а в хвост body.
    *
    * @return Инстанс DIV'а, который уже добавлен к конец body.
    */
  def initDocBody(): HTMLDivElement = {
    val div = VUtil.newDiv()
    DocumentVm().body.appendChild(div)
    div
  }

}
