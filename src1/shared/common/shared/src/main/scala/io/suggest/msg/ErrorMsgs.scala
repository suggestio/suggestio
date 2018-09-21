package io.suggest.msg

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.08.15 14:33
 * Description: Закодированные сообщения об ошибках.
 */
object ErrorMsgs extends MsgsStaticT {

  override protected def _PREFIX = "E"

  def SET_ATTR_NOT_FOUND         = E(2)

  def GEO_LOC_FAILED             = E(3)

  def GET_NODE_INDEX_FAILED      = E(4)

  def SC_FSM_EVENT_FAILED        = E(6)

  def XHR_UNEXPECTED_RESP        = E(11)

  def JSON_PARSE_ERROR           = E(16)


  def GEO_WATCH_TYPE_UNSUPPORTED   = E(19)

  def ENDLESS_LOOP_MAYBE           = E(20)

  def FOC_LOOKUP_MISSING_AD        = E(21)

  def BLE_BEACONS_API_SHUTDOWN_FAILED = E(23)
  def CANT_PARSE_IBEACON           = E(24)
  def CANT_PARSE_EDDY_STONE        = E(25)
  def BLE_BEACONS_API_AVAILABILITY_FAILED = E(26)
  def BLE_BEACONS_API_UNAVAILABLE  = E(27)
  def BLE_BEACONS_API_ENABLE_FAILED = E(28)
  def BLE_BEACONS_API_CHECK_ENABLED_FAILED = E(29)

  def UNEXPECTED_FSM_RUNTIME_ERROR = E(30)

  def FSM_MSG_PROCESS_ERROR        = E(31)

  def BLE_SCAN_ERROR               = E(33)

  def LOG_APPENDER_FAIL            = E(34)
  def ALL_LOGGERS_FAILED           = E(35)

  def CORDOVA_BLE_REQUIRE_FAILED   = E(36)

  def RME_LOGGER_REQ_FAIL          = E(37)

  def INIT_ROUTER_TARGET_RUN_FAIL  = E(39)

  def NOT_IMPLEMENTED              = E(40)

  /** Таргет почему-то пролетел мимо инициализации. */
  def INIT_ROUTER_KNOWN_TARGET_NOT_SUPPORTED = E(41)

  def UNEXPECTED_RCVR_POPUP_SRV_RESP   = E(42)

  def JS_DATE_PARSE_FAILED             = E(43)

  def ADV_GEO_FORM_ERROR               = E(44)

  def LK_NODES_FORM_ERROR              = E(45)

  /** Экшен отсеян, т.к. не имеет смысла: клиенту известны права доступа на исполнение данного экшена. */
  def ACTION_WILL_BE_FORBIDDEN_BY_SERVER = E(46)

  def NODE_NOT_FOUND                   = E(47)

  /** Неожиданно оказалось, что id рекламной карточки не задан, что недопустимо для текущего действия. */
  def AD_ID_IS_EMPTY                   = E(48)

  /** Какой-то реквест к серверу не удался. */
  def SRV_REQUEST_FAILED               = E(49)

  def TF_UNDEFINED                     = E(51)

  /** Не удалось инициализировать карту. */
  def INIT_RCVRS_MAP_FAIL          = E(52)

  def ADN_MAP_CIRCUIT_ERROR            = E(53)

  def EVENT_LISTENER_SUBSCRIBE_ERROR   = E(55)

  def JS_ROUTER_INIT_FAILED            = E(56)

  def AD_EDIT_CIRCUIT_ERROR            = E(57)

  /** На вход ожидалось изображение, а пришло что-то иное. */
  def IMG_EXPECTED                     = E(58)

  /** Ошибка во внешнем компоненте (не-react, но внутри react, например). */
  def EXT_COMP_INIT_FAILED             = E(60)

  def VIDEO_EXPECTED                   = E(62)

  def EDGE_NOT_EXISTS                  = E(63)

  def INSERT_PAYLOAD_EXPECTED          = E(64)

  def EMBEDDABLE_MEDIA_INFO_EXPECTED   = E(65)

  def DND_DROP_ERROR                   = E(67)

  def BASE64_TO_BLOB_FAILED            = E(68)

  def FILE_CLEANUP_FAIL                = E(69)

  def EXPECTED_FILE_MISSING            = E(70)

  def SHOULD_NEVER_HAPPEN              = E(71)

  def WEB_SOCKET_OPEN_FAILED           = E(72)

  def CONNECTION_ERROR                 = E(73)

  def UNSUPPORTED_VALUE_OF_ARGUMENT    = E(75)

  def GRID_CONFIGURATION_INVALID       = E(76)

  def CANONICAL_URL_FAILURE            = E(77)

  def LK_ADS_FORM_FAILED               = E(78)

  def LK_ADN_EDIT_FORM_FAILED          = E(79)

  def PLATFORM_READY_NOT_FIRED         = E(80)

  def CATCHED_CONSTRUCTOR_EXCEPTION    = E(81)

  def SCREEN_SAFE_AREA_DETECT_ERROR    = E(82)

  def CART_CIRCUIT_ERROR               = E(83)

}
