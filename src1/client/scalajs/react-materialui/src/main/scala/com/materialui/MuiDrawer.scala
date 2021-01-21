package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._

import scala.scalajs.js
import scala.scalajs.js.`|`


object MuiDrawer {

  val component = JsComponent[MuiDrawerProps, Children.Varargs, Null](Mui.Drawer)

  final def apply(props: MuiDrawerProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiDrawerPropsBase
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiDrawerClasses]
{
  val anchor: js.UndefOr[MuiDrawerAnchor] = js.undefined
  val elevation: js.UndefOr[Int] = js.undefined
  val ModalProps: js.UndefOr[MuiModalProps] = js.undefined
  val PaperProps: js.UndefOr[MuiPaperProps] = js.undefined
  val SlideProps: js.UndefOr[MuiSlideProps] = js.undefined
  val transitionDuration: js.UndefOr[Double | MuiTransitionDuration] = js.undefined
  val variant: js.UndefOr[String] = js.undefined
}
trait MuiDrawerProps extends MuiDrawerPropsBase {
  val open: js.UndefOr[Boolean] = js.undefined
}


/** The properties of the Modal component are available when variant="temporary" is set. */
trait MuiTemporaryDrawerProps
  extends MuiDrawerPropsBase
  with MuiModalPropsBase
{
  // TODO scala.js не поддерживает фиксированные значения в sjsDefined-traits. В данном случае это безопасно, т.к. дефолтовое значение совпадает с temporary.
  //override final val variant: js.UndefOr[String] = MuiDrawerVariants.temporary
}


object MuiDrawerVariant {
  final def permanent = "permanent".asInstanceOf[MuiDrawerVariant]
  final def persistent = "persistent".asInstanceOf[MuiDrawerVariant]
  final def temporary = "temporary".asInstanceOf[MuiDrawerVariant]
}


object MuiDrawerAnchor {
  final def left = "left".asInstanceOf[MuiDrawerAnchor]
  final def right = "right".asInstanceOf[MuiDrawerAnchor]
  final def top = "top".asInstanceOf[MuiDrawerAnchor]
  final def bottom = "bottom".asInstanceOf[MuiDrawerAnchor]
}

trait MuiDrawerClasses extends MuiClassesBase {
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
