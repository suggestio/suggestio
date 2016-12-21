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

  def XHR_RESP_ERROR_STATUS     = E(13)

  def TAG_SEARCH_XHR_TS_DROP    = E(14)

  def MAPBOXLG_JS_NOT_FOUND     = E(15)

  def GEO_UNEXPECTED_WATCHER_TYPE = E(17)

  def NODE_WELCOME_MISSING      = E(18)

  def MSC_STATE_URL_HASH_UNKNOWN_TOKEN = E(19)

  def GEN_NUMBER_PARSE_ERROR            = E(20)
  def NAV_PANEL_OPENED_PARSE_ERROR      = E(21)
  def SEARCH_PANEL_OPENED_PARSE_ERROR   = E(22)

  def GRID_CONT_SZ_MISSING              = E(23)

  def FOC_AD_NOT_FOUND_IN_RESP    = E(24)
  def FOC_LOOKUPED_AD_NOT_LAST    = E(25)
  def FOC_RESP_TS_UNEXPECTED      = E(26)

  def SCREEN_PX_RATIO_MISSING     = E(27)

  def POP_STATE_TO_SAME_STATE     = E(28)

  def MAP_ELEM_MISSING_BUT_EXPECTED = E(29)
  def MAP_SHUTDOWN_FAILED           = E(30)
  def MAP_DETACH_STATE_INVALID      = E(31)

  def BEACON_ACCURACY_UNKNOWN       = E(32)

  def CORDOVA_DEVICE_READY_WAIT_TIMEOUT     = E(33)

  def UNSUPPORTED_EVENT                     = E(34)

  /** Подавление повторной подписки на события со стороны какого-то листенера. */
  def EVENT_ALREADY_LISTENED_BY             = E(35)

  /** Нельзя отписаться, если не подписан на события. */
  def CANNOT_UNSUBSCRIBE_NOT_SUBSCRIBED     = E(36)

  def LEAFLET_LOCATE_CONTROL_MISSING        = E(37)

  def UNKNOWN_BLE_DEVICE                    = E(38)

  def INIT_ROUTER_NO_TARGET_SPECIFIED       = E(39)

  /**
    * BLE-девайс отсеян. т.е. например это BLE-девайс, но тип у него неподходящий
    * т.е. BeaconParser.parse() вернуло Some(Left(...)).
    */
  def FILTERED_OUT_BLE_DEVICE               = E(40)

  def BLE_BEACON_EMPTY_UID                  = E(41)

  def DATE_RANGE_FIELD_CHANGED_BUT_NO_CURRENT_RANGE_VAL = E(42)

}
