package chandu0101.scalajs.react.components
package materialui

import chandu0101.macros.tojs.JSMacro
import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.`|`

/**
 * This file is generated - submit issues instead of PR against it
 */
    
case class MuiDrawer(
  key:                js.UndefOr[String]                        = js.undefined,
  ref:                js.UndefOr[MuiDrawerM => Unit]            = js.undefined,
  /** The CSS class name of the root element. */
  className:          js.UndefOr[String]                        = js.undefined,
  /** The CSS class name of the container element. */
  containerClassName: js.UndefOr[String]                        = js.undefined,
  /** Override the inline-styles of the container element. */
  containerStyle:     js.UndefOr[CssProperties]                 = js.undefined,
  /** If true, swiping sideways when the `Drawer` is closed will not open it. */
  disableSwipeToOpen: js.UndefOr[Boolean]                       = js.undefined,
  /** If true, the `Drawer` will be docked. In this state, the overlay won't show and
     clicking on a menu item will not close the `Drawer`. */
  docked:             js.UndefOr[Boolean]                       = js.undefined,
  /** Callback function fired when the `open` state of the `Drawer` is requested to be changed.
     @param {boolean} open If true, the `Drawer` was requested to be opened.
     @param {string} reason The reason for the open or close request. Possible values are
     'swipe' for open requests; 'clickaway' (on overlay clicks),
     'escape' (on escape key press), and 'swipe' for close requests. */
  onRequestChange:    js.UndefOr[(Boolean, String) => Callback] = js.undefined,
  /** If true, the `Drawer` is opened.  Providing a value will turn the `Drawer`
     into a controlled component. */
  open:               js.UndefOr[Boolean]                       = js.undefined,
  /** If true, the `Drawer` is positioned to open from the opposite side. */
  openSecondary:      js.UndefOr[Boolean]                       = js.undefined,
  /** The CSS class name to add to the `Overlay` component that is rendered behind the `Drawer`. */
  overlayClassName:   js.UndefOr[String]                        = js.undefined,
  /** Override the inline-styles of the `Overlay` component that is rendered behind the `Drawer`. */
  overlayStyle:       js.UndefOr[CssProperties]                 = js.undefined,
  /** Override the inline-styles of the root element. */
  style:              js.UndefOr[CssProperties]                 = js.undefined,
  /** The width of the left most (or right most) area in pixels where the `Drawer` can be
     swiped open from. Setting this to `null` spans that area to the entire page
     (**CAUTION!** Setting this property to `null` might cause issues with sliders and
     swipeable `Tabs`: use at your own risk). */
  swipeAreaWidth:     js.UndefOr[Double]                        = js.undefined,
  /** The width of the `Drawer` in pixels or percentage in string format ex. `50%` to fill
     half of the window or `100%` and so on. Defaults to using the values from theme. */
  width:              js.UndefOr[String | Double]               = js.undefined,
  /** The zDepth of the `Drawer`. */
  zDepth:             js.UndefOr[ZDepth]                        = js.undefined){

  /**
    * @param children The contents of the `Drawer`
   */
  def apply(children: VdomNode*) = {
    
    val props = JSMacro[MuiDrawer](this)
    val f = JsComponent[js.Object, Children.Varargs, Null](Mui.Drawer)
    f(props)(children: _*)
  }
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
