package models.jsm

object DomWindowSpecs {

  def DELIM_DFLT = ", "
  def SB_INIT_SZ = 192
}

import DomWindowSpecs._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.15 18:37
 * Description: Модель, описывающая параметры окна для открытия через window.open(). Например, параметры попапа.
 * @see [[http://www.w3schools.com/jsref/met_win_open.asp]]
 * @param toolbar Whether or not to display the browser toolbar. IE and Firefox only.
 * @param location Whether or not to display the address field. Opera only.
 * @param directories Obsolete. Whether or not to add directory buttons. Default is yes. IE only.
 * @param status Whether or not to add a status bar.
 * @param menubar Whether or not to display the menu bar.
 * @param scrollbars Whether or not to display scroll bars. IE, Firefox & Opera only.
 * @param resizable Whether or not the window is resizable. IE only.
 * @param width Ширина окна. > 100
 * @param height Высота окна. > 0
 */
case class DomWindowSpecs(
  width       : Int,
  height      : Int,
  toolbar     : Option[Boolean] = None,
  location    : Option[Boolean] = None,
  directories : Option[Boolean] = None,
  status      : Option[Boolean] = None,
  menubar     : Option[Boolean] = None,
  scrollbars  : Option[Boolean] = None,
  resizable   : Option[Boolean] = None
) {

  def toSpecStringBuilder(delim: String = DELIM_DFLT, sb: StringBuilder = new StringBuilder(SB_INIT_SZ)): StringBuilder = {
    sb.append("width=").append(width)
      .append(delim)
      .append("height=").append(height)

    def addBoolOpt(name: String, bo: Option[Boolean]): Unit = {
      if (bo.isDefined) {
        val b = bo.get
        sb.append(delim)
          .append(name)
          .append('=')
          .append { if (b) "yes" else "no" }
      }
    }

    addBoolOpt("toolbar",       toolbar)
    addBoolOpt("location",      location)
    addBoolOpt("directories",   directories)
    addBoolOpt("status",        status)
    addBoolOpt("menubar",       menubar)
    addBoolOpt("scrollbars",    scrollbars)
    addBoolOpt("resizable",     resizable)

    sb
  }

  /** Конвертация в строку. */
  def toSpecString(delim: String = DELIM_DFLT): String = {
    toSpecStringBuilder(delim)
      .toString()
  }

  override def toString: String = {
    val sb = new StringBuilder(SB_INIT_SZ, "DomWindowSpecs(")
    toSpecStringBuilder(",", sb)
      .append(')')
      .toString()
  }
}
