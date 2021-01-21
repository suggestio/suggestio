package com

import japgolly.univeq._

import scala.scalajs.js
import scala.scalajs.js.|

package object materialui {

  /* todo: need generate script to become more clever */
  private[materialui] implicit class A(m: Mui.type) {
    def MuiThemeProvider = m.Styles.MuiThemeProvider
  }

  type MuiInputValue_t = String | Double | Boolean

  type Component_t = String | js.Function | js.Object

  type MuiDrawerVariant <: String
  type MuiSlideDirection <: String
  type MuiDrawerAnchor <: String
  type MuiModalCloseReason <: String

  @inline implicit def muiModelCloseReasonUe: UnivEq[MuiModalCloseReason] = UnivEq.force

}
