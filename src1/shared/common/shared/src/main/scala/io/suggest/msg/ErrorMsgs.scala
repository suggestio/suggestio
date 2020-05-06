package io.suggest.msg

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.08.15 14:33
 * Description: Закодированные сообщения об ошибках.
 */
object ErrorMsgs {

  def GEO_LOC_FAILED = "Geolocation.failed"

  def GET_NODE_INDEX_FAILED = "Get.node.index.failed"

  def SC_FSM_EVENT_FAILED = "Sc.fsm.event.failed"

  def XHR_UNEXPECTED_RESP = "Http.response.unexpected"

  def JSON_PARSE_ERROR = "Json.parse.error"


  def GEO_WATCH_TYPE_UNSUPPORTED = "Geo.watch.type.unsupported"

  def ENDLESS_LOOP_MAYBE = "Endless.loop.maybe"

  def FOC_LOOKUP_MISSING_AD = "Focused.lookup.ad.missing"

  def BLE_BEACONS_API_SHUTDOWN_FAILED = "Ble.beacons.api.Shutdown.failed"
  def CANT_PARSE_EDDY_STONE = "Cannot.parse.eddy.stone"
  def BLE_BEACONS_API_AVAILABILITY_FAILED = "Ble.beacons.api.Availability.failed"
  def BLE_BEACONS_API_UNAVAILABLE  = "Ble.beacons.api.Unavailable"
  def BLE_BEACONS_API_ENABLE_FAILED = "Ble.beacons.api.Enable.failed"
  def BLE_BEACONS_API_CHECK_ENABLED_FAILED = "Ble.beacons.api.Check.enabled.failed"
  def BLE_BT_DISABLED = "Ble.Bt.disabled"

  def UNEXPECTED_FSM_RUNTIME_ERROR = "Unexpected.fsm.runtime.error"

  def BLE_SCAN_ERROR = "Ble.scan.error"

  def LOG_APPENDER_FAIL = "Log.appender.failed"
  def ALL_LOGGERS_FAILED = "All.loggers.failed"

  def CORDOVA_BLE_REQUIRE_FAILED = "Cordova.ble.require.failed"

  def RME_LOGGER_REQ_FAIL = "Remote.logger.req.fail"

  def INIT_ROUTER_TARGET_RUN_FAIL = "Init.router.target.run.fail"

  def NOT_IMPLEMENTED = "Not.implemented"

  /** Таргет почему-то пролетел мимо инициализации. */
  def INIT_ROUTER_KNOWN_TARGET_NOT_SUPPORTED = "Init.router.known.target.not.supported"

  def UNEXPECTED_RCVR_POPUP_SRV_RESP = "Unexpected.receiver.popup.server.resp"

  def JS_DATE_PARSE_FAILED = "Js.date.parse.failed"

  def ADV_GEO_FORM_ERROR = "Adv.geo.form.error"

  def LK_NODES_FORM_ERROR = "Lk.nodes.form.error"

  /** Экшен отсеян, т.к. не имеет смысла: клиенту известны права доступа на исполнение данного экшена. */
  def ACTION_WILL_BE_FORBIDDEN_BY_SERVER = "Action.will.be.forbidden.by.server"

  def NODE_NOT_FOUND = "Node.not.found"

  /** Неожиданно оказалось, что id рекламной карточки не задан, что недопустимо для текущего действия. */
  def AD_ID_IS_EMPTY = "Ad.id.is.empty"

  /** Какой-то реквест к серверу не удался. */
  def SRV_REQUEST_FAILED = "Server.request.failed"

  def TF_UNDEFINED = "Tariff.undefined"

  /** Не удалось инициализировать карту. */
  def INIT_RCVRS_MAP_FAIL = "Receivers.map.init.failed"

  def ADN_MAP_CIRCUIT_ERROR = "Adn.map.circuit.error"

  def EVENT_LISTENER_SUBSCRIBE_ERROR = "Event.listener.subscribe.error"

  def AD_EDIT_CIRCUIT_ERROR = "Ad.edit.circuit.error"

  /** На вход ожидалось изображение, а пришло что-то иное. */
  def IMG_EXPECTED = "Image.expected"

  /** Ошибка во внешнем компоненте (не-react, но внутри react, например). */
  def EXT_COMP_INIT_FAILED = "External.component.init.failed"

  def VIDEO_EXPECTED = "Video.expected"

  def EDGE_NOT_EXISTS = "Edge.not.exists"

  def INSERT_PAYLOAD_EXPECTED = "Insert.payload.expected"

  def EMBEDDABLE_MEDIA_INFO_EXPECTED = "Embeddable.media.info.expected"

  def BASE64_TO_BLOB_FAILED = "Base64.to.blob.failed"

  def FILE_CLEANUP_FAIL = "File.cleanup.fail"

  def EXPECTED_FILE_MISSING = "Expected.file.missing"

  def SHOULD_NEVER_HAPPEN = "Should.never.happen"

  def WEB_SOCKET_OPEN_FAILED = "Web.socket.open.failed"

  def CONNECTION_ERROR = "Connection.error"

  def UNSUPPORTED_VALUE_OF_ARGUMENT = "Unsupported.value.of.argument"

  def GRID_CONFIGURATION_INVALID = "Grid.configuration.invalid"

  def CANONICAL_URL_FAILURE = "Canonical.url.failure"

  def LK_ADS_FORM_FAILED = "Lk.ads.form.failed"

  def LK_ADN_EDIT_FORM_FAILED = "Lk.adn.edit.form.failed"

  def PLATFORM_READY_NOT_FIRED = "Platform.ready.not.fired"

  def PLATFORM_ID_FAILURE = "Platform.id.failure"

  def CATCHED_CONSTRUCTOR_EXCEPTION = "Constructor.exception.catched"

  def SCREEN_SAFE_AREA_DETECT_ERROR = "Screen.safe.area.detect.error"

  def CART_CIRCUIT_ERROR = "Cart.circuit.error"

  def SYS_MDR_CIRCUIT_ERROR = "Sys.mdr.circuit.error"

  def KV_STORAGE_CHECK_FAILED = "Kv.storage.check.failed"

  def KV_STORAGE_ACTION_FAILED = "Kv.storage.action.failed"

  def CONF_SAVE_FAILED = "Conf.save.failed"

  def CACHING_ERROR = "Cache.error"

  /** Ошибка вызова нативного API. */
  def PERMISSION_API_FAILED = "Permission.api.failed"

  /** Логика работы неверна или завела в тупик. */
  def PERMISSION_API_LOGIC_INVALID = "Permission.api.logic.invalid"

  /** Косяк в форме логина. */
  def LOGIN_FORM_ERROR = "Login.form.error"

  /** Отсутствуют GridBuild-данные плитки, когда они так нужны.
    * Простая плитка из JdR далеко не всегда нужна: в выдаче своя плитка, а в ЛК в основном рендер без плитки,
    * поэтому данные плитки при рендере JdR() обычно отсутствуют, и это нормально.
    */
  def GRID_BUILD_RES_MISSING = "Grid.build.res.missing"

  def MESSAGES_FAILURE = "Messages.failure"

  def JD_TREE_UNEXPECTED_ROOT_TAG = "Jd.tree.Unexpected.root.tag"
  def JD_TREE_UNEXPECTED_CHILDREN = "Jd.tree.Unexpected.children"
  def JD_TREE_UNEXPECTED_ID = "Jd.tree.unexpected.id"


  // ------------------------------------------------------------------
  // warnings
  // ------------------------------------------------------------------

  def NO_SCREEN_VSZ_DETECTED = "No.screen.vsz.detected"

  def FSM_SIGNAL_UNEXPECTED = "Fsm.signal.unexpected"


  def GEO_UNEXPECTED_WATCHER_TYPE = "Geo.unexpected.watcher.type"

  def SC_URL_HASH_UNKNOWN_TOKEN = "Sc.url.hash.Unknown.token"

  def SCREEN_PX_RATIO_MISSING = "Screen.pixel.ratio.missing"

  def BEACON_ACCURACY_UNKNOWN = "Beacon.accuracy.unknown"

  /** Подавление повторной подписки на события со стороны какого-то листенера. */
  def EVENT_ALREADY_LISTENED_BY = "Event.already.listened.by"

  def INIT_ROUTER_NO_TARGET_SPECIFIED = "Init.router.no.target.specified"

  def BLE_BEACON_EMPTY_UID = "Ble.beacon.empty.uid"

  def DATE_RANGE_FIELD_CHANGED_BUT_NO_CURRENT_RANGE_VAL = "Date.range.field.changed.but.no.current.range.val"

  def GJ_PROPS_EMPTY_OR_MISS = "GeoJson.props.empty.or.missing"

  def GEO_JSON_GEOM_COORD_UNEXPECTED_ELEMENT = "GeoJson.geom.coord.unexpected.element"

  /** Получен ответ сервера на уже неактуальный запрос. Чисто информационная мессага. */
  def SRV_RESP_INACTUAL_ANYMORE = "Server.response.inactual.anymore"

  /** Отвергнута попытка обновить данные внутри Pot, который вообще пустой и не ожидает данных. */
  def REFUSED_TO_UPDATE_EMPTY_POT_VALUE = "Refused.to.update.Empty.pot"

  /** Какое-то действие отфильтровано, т.к. система ожидает исполнения реквеста по смежному или этому же действию. */
  def REQUEST_STILL_IN_PROGRESS = "Request.still.in.progress"

  /** Что-то не удалось провалидировать. */
  def VALIDATION_FAILED = "Validation.failed"

  /** Неожиданно пустой документ. */
  def UNEXPECTED_EMPTY_DOCUMENT = "Unexpected.empty.document"

  /** Отработка какой-то проблемы, связанные с отсутствием завершающего \n в документе. */
  def QDELTA_FINAL_NEWLINE_PROBLEM = "QDelta.final.newline.problem"

  /** Файл (изображение) потерялось куда-то в ходе блобификации. */
  def SOURCE_FILE_NOT_FOUND = "Source.file.not.found"

  /** Не удаётся закрыть файл/коннекшен/что-то. */
  def CANNOT_CLOSE_SOMETHING = "Cannot.close.something"

  def UNKNOWN_CONNECTION = "Unknown.connection"

  def IMG_URL_EXPECTED = "Image.url.expected"

  def INACTUAL_NOTIFICATION = "Inactual.notification"

  /** Событие проигнорено (подавлено), потому что причин для реагирования недостаточно. */
  def SUPPRESSED_INSUFFICIENT = "Suppressed.insufficient"

  def NODE_PATH_MISSING_INVALID = "Node.path.missing.or.invalid"

  /** Какой-то шаг в ходе инициализации тихо пошёл по сценарию,
    * неожиданному с точки зрения над-стоящей логики. */
  def INIT_FLOW_UNEXPECTED = "Init.flow.unexpected"

  /** Ошибка в какой-либо форме. */
  def FORM_ERROR = "Form.error"

  def CONTENT_TYPE_UNEXPECTED = "Content.type.unexpected"

  def NATIVE_API_ERROR = "Native.api.error"

  def NOTIFICATION_API_UNAVAILABLE = "Notification.api.unavailable"

  def DAEMON_BACKEND_FAILURE = "Daemon.backend.failure"

  def DIAGNOSTICS_RETRIEVE_FAIL = "Diag.retrive.fail"

}
