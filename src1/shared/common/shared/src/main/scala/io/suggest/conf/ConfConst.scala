package io.suggest.conf

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.18 18:35
  * Description: Ключи для dom.window.LocalStorage.
  */
object ConfConst {

  def SC_PREFIX = "sc."

  /** Инициализация конфига. */
  def SC_INIT_KEY = SC_PREFIX + "init"

  /** Первый запуск. */
  def SC_FIRST_RUN = SC_PREFIX + "first.run"

  def IS_TOUCH_DEV = "device.touch.is"

  def SC_INDEXES_RECENT = "sc.indexes.recent"

}
