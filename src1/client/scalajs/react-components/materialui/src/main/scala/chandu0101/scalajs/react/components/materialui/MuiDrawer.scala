package chandu0101.scalajs.react.components
package materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._

import scala.scalajs.js
import scala.scalajs.js.`|`


object MuiDrawer {

  val component = JsComponent[MuiDrawerProps, Children.Varargs, Null](Mui.Drawer)

  def apply(props: MuiDrawerProps = MuiDrawerProps.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiDrawerPropsBase
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiDrawerClasses]
{
  val anchor: js.UndefOr[String] = js.undefined
  val elevation: js.UndefOr[Int] = js.undefined
  val ModalProps: js.UndefOr[MuiModalProps] = js.undefined
  val PaperProps: js.UndefOr[MuiPaperProps] = js.undefined
  val SlideProps: js.UndefOr[MuiSlideProps] = js.undefined
  val transitionDuration: js.UndefOr[Double | MuiTransitionDuration] = js.undefined
  val variant: js.UndefOr[String] = js.undefined
}
trait MuiDrawerProps extends MuiDrawerPropsBase {
  val onClose: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined
  val open: js.UndefOr[Boolean] = js.undefined
}
object MuiDrawerProps extends MuiPropsBaseStatic[MuiDrawerProps]


/** The properties of the Modal component are available when variant="temporary" is set. */
trait MuiTemporaryDrawerProps
  extends MuiDrawerPropsBase
  with MuiModalPropsBase
{
  // TODO scala.js не поддерживает фиксированные значения в sjsDefined-traits. В данном случае это безопасно, т.к. дефолтовое значение совпадает с temporary.
  //override final val variant: js.UndefOr[String] = MuiDrawerVariants.temporary
}


object MuiDrawerVariants {
  val permanent = "permanent"
  val persistent = "persistent"
  val temporary = "temporary"
}


object MuiDraweAnchors {
  val left = "left"
  val right = "right"
  val top = "top"
  val bottom = "bottom"
}

trait MuiDrawerClasses extends js.Object {
  // no root css here
  val docked: js.UndefOr[String] = js.undefined
  val paper: js.UndefOr[String] = js.undefined
  val paperAnchorLeft: js.UndefOr[String] = js.undefined
  val paperAnchorRight: js.UndefOr[String] = js.undefined
  val paperAnchorTop: js.UndefOr[String] = js.undefined
  val paperAnchorBottom: js.UndefOr[String] = js.undefined
  val paperAnchorDockedLeft: js.UndefOr[String] = js.undefined
  val paperAnchorDockedTop: js.UndefOr[String] = js.undefined
  val paperAnchorDockedRight: js.UndefOr[String] = js.undefined
  val paperAnchorDockedBottom: js.UndefOr[String] = js.undefined
  val modal: js.UndefOr[String] = js.undefined
}


@js.native
trait MuiDrawerM extends js.Object {
  def close(reason: String): Unit = js.native

  def disableSwipeHandling(): Unit = js.native

  def enableSwipeHandling(): Unit = js.native

  def getMaxTranslateX(): Int = js.native

  def getStyles(): CssProperties = js.native

  def getTranslateMultiplier(): Int = js.native

  def getTranslateX(currentX: Int): Int = js.native

  def getTranslatedWidth(): Double = js.native

  def open(reason: String): Unit = js.native

  def removeBodyTouchListeners(): Unit = js.native

  def setPosition(position: Int): Unit = js.native

  def shouldShow(): Boolean = js.native
}
