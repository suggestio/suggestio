package io.suggest.sjs.common.msg

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.08.15 9:16
 * Description: Коды warning'ов в sjs-либах.
 */
object WarnMsgs extends MsgsStaticT {

  override protected def _PREFIX = "W"

  def NOT_YET_IMPLEMENTED       = E(0)

  def NO_OUTER_HTML_SUPPORT     = E(1)

  def NO_SCREEN_VSZ_DETECTED    = E(2)

  def INDEX_RESP_TOO_OLD        = E(3)

  def WATCH_POSITION_EMPTY      = E(4)

  def FTS_SD_MISSING            = E(5)

}
