package com.materialui

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.06.2020 18:35
  */

/** JSON fop [[MuiSnackBarProps.anchorOrigin]]. */
trait MuiAnchorOrigin extends js.Object {
  val vertical: js.UndefOr[String] = js.undefined
  val horizontal: js.UndefOr[String] = js.undefined
}


object MuiAnchorOrigin {
  val left = "left"
  val right = "right"
  val center = "center"
  val top = "top"
  val bottom = "bottom"
}


trait MuiTopLeft extends js.Object {
  val top: js.UndefOr[Double] = js.undefined
  val left: js.UndefOr[Double] = js.undefined
}


object MuiAnchorReferences {
  type AnchorReference_t <: js.Any
  def anchorEl = "anchorEl".asInstanceOf[AnchorReference_t]
  def anchorPosition = "anchorPosition".asInstanceOf[AnchorReference_t]
  def none = "none".asInstanceOf[AnchorReference_t]
}