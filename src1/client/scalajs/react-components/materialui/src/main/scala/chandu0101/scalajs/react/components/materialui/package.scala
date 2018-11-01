package chandu0101.scalajs.react.components

import scala.scalajs.js
import scala.scalajs.js.|

package object materialui {
  type RowId    = Int
  type ColumnId = Int

  /* todo: need generate script to become more clever */
  private[materialui] implicit class A(m: Mui.type) {
    def MuiThemeProvider = m.Styles.MuiThemeProvider
  }

  type TouchTapEvent   = raw1.TouchTapEvent[org.scalajs.dom.Node]

  type MuiInputValue_t = String | Double | Boolean

  type Component_t = String | js.Function | js.Object

}
