package io.suggest.msg

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

  def GJ_PROPS_EMPTY_OR_MISS                = E(43)

  def GEO_JSON_GEOM_COORD_UNEXPECTED_ELEMENT = E(44)

  /** Получен ответ сервера на уже неактуальный запрос. Чисто информационная мессага. */
  def SRV_RESP_INACTUAL_ANYMORE              = E(45)

  /** Отвергнута попытка обновить данные внутри Pot, который вообще пустой и не ожидает данных. */
  def REFUSED_TO_UPDATE_EMPTY_POT_VALUE      = E(46)

  /** Какое-то действие отфильтровано, т.к. система ожидает исполнения реквеста по смежному или этому же действию. */
  def REQUEST_STILL_IN_PROGRESS              = E(47)

  /** Что-то не удалось провалидировать. */
  def VALIDATION_FAILED                      = E(48)

  /** Некая транзакция уже открыта, хотя запрошено неожиданное открытие транзакции. */
  def TXN_ALREADY_OPENED                     = E(49)

  /** Транзакция была закрыта, но никаких операций не произошло. */
  def CLOSED_TXN_EMPTY                       = E(50)

  /** Транзакция уже закрыта или ещё не открыта, но запрошено действие, подразумевающее наличие открытой транзакции. */
  def TXN_NOT_OPENED                         = E(51)

  /** Неожиданно пустой документ. */
  def UNEXPECTED_EMPTY_DOCUMENT              = E(52)

  /** Отработка какой-то проблемы, связанные с отсутствием завершающего \n в документе. */
  def QDELTA_FINAL_NEWLINE_PROBLEM           = E(53)

  def DND_DROP_UNSUPPORTED                   = E(54)

  /** Ожидалось, что эффектов не будет, а они есть. */
  def UNEXPECTED_EFFECTS                     = E(55)

  /** Файл (изображение) потерялось куда-то в ходе блобификации. */
  def SOURCE_FILE_NOT_FOUND   = E(56)

  /** Не удаётся закрыть файл/коннекшен/что-то. */
  def CANNOT_CLOSE_SOMETHING                 = E(57)

  def UNKNOWN_CONNECTION                     = E(58)

  def MISSING_UPDATED_EDGE                   = E(59)

}
