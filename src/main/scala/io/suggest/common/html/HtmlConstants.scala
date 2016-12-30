package io.suggest.common.html

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.12.15 19:55
 * Description:
 */
object HtmlConstants {

  /** Префикс кастомных html-атрибутов. */
  def ATTR_PREFIX             = "data-"

  /** Имя атрибута с id узла. */
  def ATTR_NODE_ID            = ATTR_PREFIX + "node-id"

  /** Обычный пробел. Он используется в куче мест. */
  val SPACE                   = " "
  val COMMA                   = ","

  val CHECKMARK               = "✓"

  val TAG_PREFIX              = "#"

}
