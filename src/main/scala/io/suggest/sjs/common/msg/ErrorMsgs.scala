package io.suggest.sjs.common.msg

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.08.15 14:33
 * Description:
 */
object ErrorMsgs {

  private def E(i: Int): String = "E" + i

  def FOC_PRELOAD_REQUEST_FAILED = E(1)

  def SET_ATTR_NOT_FOUND         = E(2)

  def BSS_GEO_LOC_FAILED         = E(3)

  def GET_NODE_INDEX_FAILED      = E(4)

  def FIND_ADS_REQ_FAILED        = E(5)

  def SC_FSM_EVENT_FAILED        = E(6)

  def FOC_FIRST_REQ_FAILED       = E(7)

  def FOC_ANSWER_ACTION_MISSING  = E(8)

  def FOC_ANSWER_ACTION_INVALID  = E(9)

}
