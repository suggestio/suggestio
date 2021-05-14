package com

import japgolly.univeq._

import scala.scalajs.js
import scala.scalajs.js.|

package object materialui {

  type MuiInputValue_t = String | Double | Boolean

  type Component_t = String | js.Function | js.Object

  type MuiDrawerVariant <: String
  type MuiSlideDirection <: String
  type MuiDrawerAnchor <: String
  type MuiModalCloseReason <: String

  @inline implicit def muiModelCloseReasonUe: UnivEq[MuiModalCloseReason] = UnivEq.force

}
