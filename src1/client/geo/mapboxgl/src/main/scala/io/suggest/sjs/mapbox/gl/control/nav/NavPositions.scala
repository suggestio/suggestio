package io.suggest.sjs.mapbox.gl.control.nav

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 15:58
  * Description: Possible values for [[NavOptions]].position.
  */
object NavPositions {

  private def TOP     = "top"
  private def BOTTOM  = "bottom"
  private def RIGHT   = "right"
  private def LEFT    = "left"
  private def DELIM   = "-"

  def TOP_RIGHT     = TOP + DELIM + RIGHT
  def TOP_LEFT      = TOP + DELIM + LEFT
  def BOTTOM_RIGHT  = BOTTOM + DELIM + RIGHT
  def BOTTOM_LEFT   = BOTTOM + DELIM + LEFT

}
