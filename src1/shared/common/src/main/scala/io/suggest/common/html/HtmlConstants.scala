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

  val ELLIPSIS                = "…"

  val CHECKMARK               = "✓"

  val DIEZ                    = "#"

  def NEWLINE_UNIX            = '\n'

  @inline
  def TAG_PREFIX              = DIEZ

  val NBSP                    = '\u00A0'
  val NBSP_STR                = NBSP.toString


  object Input {

    val checkbox = "checkbox"
    val text = "text"
    val radio = "radio"
    val submit = "submit"
    val file = "file"

  }

  object Target {
    val blank = "_blank"
  }

}
