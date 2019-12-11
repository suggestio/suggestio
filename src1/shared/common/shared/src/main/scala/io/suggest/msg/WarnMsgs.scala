package io.suggest.msg

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.08.15 9:16
 * Description: Коды warning'ов в sjs-либах.
 */
object WarnMsgs extends MsgsStaticT {

  override protected def _PREFIX = "W"

  def NO_SCREEN_VSZ_DETECTED    = E(2)

  def FSM_SIGNAL_UNEXPECTED     = E(7)


  def GEO_UNEXPECTED_WATCHER_TYPE = E(17)

  def MSC_STATE_URL_HASH_UNKNOWN_TOKEN = E(19)

  def SCREEN_PX_RATIO_MISSING     = E(27)

  def BEACON_ACCURACY_UNKNOWN       = E(32)

  /** Подавление повторной подписки на события со стороны какого-то листенера. */
  def EVENT_ALREADY_LISTENED_BY             = E(35)

  def INIT_ROUTER_NO_TARGET_SPECIFIED       = E(39)

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

  /** Неожиданно пустой документ. */
  def UNEXPECTED_EMPTY_DOCUMENT              = E(52)

  /** Отработка какой-то проблемы, связанные с отсутствием завершающего \n в документе. */
  def QDELTA_FINAL_NEWLINE_PROBLEM           = E(53)

  /** Файл (изображение) потерялось куда-то в ходе блобификации. */
  def SOURCE_FILE_NOT_FOUND   = E(56)

  /** Не удаётся закрыть файл/коннекшен/что-то. */
  def CANNOT_CLOSE_SOMETHING                 = E(57)

  def UNKNOWN_CONNECTION                     = E(58)

  def IMG_URL_EXPECTED                       = E(59)

  def INACTUAL_NOTIFICATION                  = E(60)

  /** Событие проигнорено (подавлено), потому что причин для реагирования недостаточно. */
  def SUPPRESSED_INSUFFICIENT                = E(61)

  def NODE_PATH_MISSING_INVALID              = E(63)

  /** Какой-то шаг в ходе инициализации тихо пошёл по сценарию,
    * неожиданному с точки зрения над-стоящей логики. */
  def INIT_FLOW_UNEXPECTED                   = E(64)

}
