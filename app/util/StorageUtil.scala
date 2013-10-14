package util

import play.api.Play.current

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.10.13 19:04
 * Description: Утиль для переключения между хранилищами (backend'ами моделей).
 */

object StorageUtil extends Logs {

  import LOGGER._


  // Модели могут переключаться между HBase, так и JsonDFS. Для продакшена подоходит только HBase, но не очень подходит
  // для локалхоста. Переключение между hbase и fs происходит через конфиг, а модели при запуске выбирают свои backend'ы.
  object StorageType extends Enumeration {
    type StorageType = Value
    val HBASE, DFS = Value
  }

  import StorageType._

  private val storageDflt = DFS

  /** Выбор хранилища, которую будут использовать модели. */
  val STORAGE: StorageType = {
    current.configuration.getString("storage").map(_.toUpperCase) match {
      case None =>
        val v = storageDflt
        warn(s"Storage backend not defined in application.conf. Using 'storage' = '$v'.")
        v

      case Some(v)  => StorageType.withName(v)
    }
  }

}
