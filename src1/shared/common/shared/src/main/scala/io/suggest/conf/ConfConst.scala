package io.suggest.conf

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.18 18:35
  * Description: Ключи для dom.window.LocalStorage.
  */
object ConfConst {

  final def SC_PREFIX = "sc."

  /** Ключ в user-хранилищи по init-контейнеру конфига выдачи. */
  final def SC_INIT_KEY = SC_PREFIX + "init"

  /** Ключ хранилища настроек выдачи. */
  final def SC_SETTINGS_KEY = SC_PREFIX + "settings"

  object ScSettings {
    private final def _ENABLED = ".enabled"

    // Ключи внутри settings-контейнера.
    final def BLUETOOTH_BEACONS_ENABLED   = "bluetooth.beacons" + _ENABLED
    final def BLUETOOTH_BEACONS_BACKGROUND_SCAN   = "bluetooth.beacons.background.scan" + _ENABLED
    final def LOCATION_ENABLED            = "location" + _ENABLED
    final def NOTIFICATIONS_ENABLED       = "notifications" + _ENABLED
  }


  /** Первый запуск. */
  final def SC_FIRST_RUN = SC_PREFIX + "first.run"

  final def IS_TOUCH_DEV = "device.touch.is"

  final def SC_INDEXES_RECENT = "sc.indexes.recent"

}
