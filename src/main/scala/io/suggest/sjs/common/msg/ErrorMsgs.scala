package io.suggest.sjs.common.msg

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.08.15 14:33
 * Description: Закодированные сообщения об ошибках.
 */
object ErrorMsgs extends MsgsStaticT {

  override protected def _PREFIX = "E"

  def FOC_PRELOAD_REQUEST_FAILED = E(1)

  def SET_ATTR_NOT_FOUND         = E(2)

  def GEO_LOC_FAILED         = E(3)

  def GET_NODE_INDEX_FAILED      = E(4)

  def FIND_ADS_REQ_FAILED        = E(5)

  def SC_FSM_EVENT_FAILED        = E(6)

  def FOC_FIRST_REQ_FAILED       = E(7)

  def FOC_ANSWER_ACTION_MISSING  = E(8)

  def FOC_ANSWER_ACTION_INVALID  = E(9)

  def OPEN_AD_ID_MUST_BE_NONE    = E(10)

  def XHR_UNEXPECTED_RESP        = E(11)

  def CANT_ADD_TAG_SERVER_ERROR  = E(12)

  def TAGS_SEARCH_REQ_FAILED     = E(13)

  def ADV_DIRECT_FORM_PRICE_URL_METHOD_MISS = E(14)

  def ADV_DIRECT_FORM_PRICE_FAIL = E(15)

  def JSON_PARSE_ERROR           = E(16)


  def NOT_A_GEO_JSON_FEATURE_GROUP = E(17)

  def QUEUE_OVERLOADED             = E(18)

  def GEO_WATCH_TYPE_UNSUPPORTED   = E(19)

}
