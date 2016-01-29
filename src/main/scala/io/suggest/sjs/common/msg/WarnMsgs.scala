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

  def BACK_TO_UNDEFINED_NODE    = E(6)

  def FSM_SIGNAL_UNEXPECTED     = E(7)

  def INPUT_UNFOCUSED_EVENT     = E(8)


  def RAD_MAP_CONT_MISSING      = E(9)

  def RAD_MAP_NO_START_STATE    = E(10)


  def DT_PICKER_ARGS_MISSING    = E(11)


  def ADV_DIRECT_XHR_TS_DROP    = E(12)

}
