package io.suggest.lk.r.popup

import diode.react.ModelProxy
import diode.{FastEq, UseValueEq}
import io.suggest.css.Css
import io.suggest.lk.m.CloseAllPopups
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.doc.DocumentVm
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.raw.HTMLDivElement
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 18:15
  * Description: React-компонент контейнера react-попапов.
  */
object PopupsContR {

  type Props = ModelProxy[PropsVal]

  case class PropsVal(
                       visible  : Boolean,
                       css      : String  = ""
                     )
    extends UseValueEq

  implicit object PopContPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.visible ==* b.visible) &&
      (a.css ===* b.css)
    }
  }


  class Backend($: BackendScope[Props, Unit]) {

    /** Callback клика по подложке попапов. */
    private def onBgClick: Callback = {
      dispatchOnProxyScopeCB( $, CloseAllPopups )
    }

    def render(propsProxy: Props, pc: PropsChildren): VdomElement = {
      val p = propsProxy()
      <.aside(
        ^.classSet1(
          Css.flat( Css.Lk.Popup.POPUPS, Css.Lk.Popup.POPUPS_CONTAINER, p.css ),
          Css.Display.VISIBLE -> p.visible,
          Css.Display.BLOCK   -> p.visible
        ),

        ^.onClick --> onBgClick,

        // Отрендерить все попапы:
        pc
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackendWithChildren[Backend]
    .build

  def apply(props: Props)(children: VdomNode*) = component(props)(children: _*)

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
