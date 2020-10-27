package com.github.balloob.react.sidebar

import io.suggest.sjs.common.vm.wnd.WindowStub
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.17 11:57
  * Description: react-sidebar scala wrapper.
  */
object Sidebar {

  // workaround on scalajs-1.3.0 for https://github.com/scalacenter/scalajs-bundler/issues/381#issuecomment-717258485
  js.constructorOf[SidebarJs]

  val component = JsComponent[SidebarProps, Children.Varargs, SidebarState]( js.constructorOf[SidebarForceIosJs] )

  def apply(props: SidebarProps)(children: VdomNode*) = component( props )(children: _*)

}


@JSImport("react-sidebar", JSImport.Default)
@js.native
class SidebarJs(props: SidebarProps) extends js.Object {
  def componentDidMount(): Unit = js.native
  def componentDidUpdate(): Unit = js.native
  def onTouchStart(e: ReactTouchEvent): Unit = js.native
  def onTouchMove(e: ReactTouchEvent): Unit = js.native
  def setState(s: SidebarState): Unit = js.native
  def saveSidebarWidth(): Unit = js.native
  def render(): raw.React.Element = js.native
}


/** Отклонение неотключаемого запрета работать на ios.
  * См. README и https://github.com/balloob/react-sidebar/commit/01dd3478af3349a409882bdd220ead6b3e3791c5
  */
class SidebarForceIosJs(props: SidebarProps) extends SidebarJs(props) {
  override def componentDidMount(): Unit = {
    setState(new SidebarState {
      // TODO Возможно, надо отключать в браузере на iOS, но не в кордове.
      override val dragSupported = WindowStub(dom.window).ontouchstart.nonEmpty
    })
    saveSidebarWidth()
  }
}


/** State js-object for [[SidebarJs]]. */
trait SidebarState extends js.Object {
  val sidebarWidth: js.UndefOr[Int] = js.undefined
  val touchIdentifier, touchStartX, touchCurrentX: js.UndefOr[js.Any] = js.undefined
  val dragSupported: Boolean
}
