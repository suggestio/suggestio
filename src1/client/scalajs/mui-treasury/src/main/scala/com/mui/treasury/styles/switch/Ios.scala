package com.mui.treasury.styles.switch

import com.materialui.{MuiSwitchClasses, MuiTheme}
import com.mui.treasury.styles.PKG_STYLES

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.07.2020 16:48
  */
@js.native @JSImport( PKG_STYLES + "/switch/ios", JSImport.Namespace )
object Ios extends js.Object {
  def iosSwitchStyles(theme: MuiTheme): MuiSwitchClasses = js.native
  def useIosSwitchStyles(props: js.Object = js.native): MuiSwitchClasses = js.native
}
