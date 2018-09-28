package io.suggest.common.html

import io.suggest.ad.blk.BlockWidths
import io.suggest.common.geom.d2.{ISize2di, MSize2di}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.12.15 19:55
 * Description:
 */
object HtmlConstants {

  final def DATA = "data"
  final def BLOB = "blob"


  object Proto {
    final def DATA_ = DATA + COLON
    final def BLOB_ = BLOB + COLON
  }


  /** Префикс кастомных html-атрибутов. */
  final def ATTR_PREFIX       = DATA + "-"

  /** Имя атрибута с id узла. */
  def ATTR_NODE_ID            = ATTR_PREFIX + "node-id"

  /** Обычный пробел. Он используется в куче мест. */
  val SPACE                   = " "
  val COMMA                   = ","

  val ELLIPSIS                = "…"

  val CHECKMARK               = "✓"

  val DIEZ                    = "#"

  val NEWLINE_UNIX            = '\n'

  val COLON                   = ":"

  val SLASH                   = "/"

  val `(`                     = "("
  val `)`                     = ")"

  val `[`                     = "["
  val `]`                     = "]"

  val PLUS                    = "+"
  val MINUS                   = "-"
  val UNDERSCORE              = "_"
  val ASTERISK                = "*"

  val LESSER                  = "<"
  val GREATER                 = ">"

  val `.`                     = "."
  val `~`                     = "~"
  val PIPE                    = "|"

  @inline
  def TAG_PREFIX              = DIEZ

  val NBSP                    = '\u00A0'
  val NBSP_STR                = NBSP.toString


  object Input {

    val input = "input"
    val checkbox = "checkbox"
    val select = "select"
    val text = "text"
    val radio = "radio"
    val submit = "submit"
    val file = "file"
    val range = "range"

  }

  object Target {
    val blank = "_blank"
  }

  /** Константы для iframe'ов. */
  object Iframes {

    /** Дефолтовые размеры для iframe в CSSpx. */
    final def whCsspxDflt = {
      val w = BlockWidths.NARROW.value
      MSize2di(width = w, height = w / 2)
    }

  }


  /** Константы для link-тега. */
  object Links {

    /** Стандартные значения аттрибута rel. */
    object Rels {

      // Иконки сайта или приложения.
      final def ICON              = "icon"
      final def SHORTCUT_ICON     = "shortcut " + ICON
      final def APPLE_TOUCH       = "apple-touch"
      final def APPLE_TOUCH_ICON  = APPLE_TOUCH + "-" + ICON

    }

  }


}
