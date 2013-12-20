package io.suggest.util

import com.typesafe.config.Config

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.12.13 14:19
 * Description:
*/

// Модели могут переключаться между HBase, так и JsonDFS. Для продакшена подоходит только HBase, но не очень подходит
// для локалхоста. Переключение между hbase и fs происходит через конфиг, а модели при запуске выбирают свои backend'ы.
object StorageType extends Enumeration {
  type StorageType = Value
  val HBASE, DFS = Value
}

import StorageType._

object SioModelUtil extends StorageTypeFromConfigT {

  protected def getConfig: Config = MyConfig.CONFIG.underlying

  /** Ключ в конфигах, значение которого описывает испоьлзуемый backend в моделях. */
  val STORAGE_TYPE_CONFIG_KEY = "storage"

  private def getLogger = new LogsImpl(getClass)
  private def getStorageDflt = DFS

  def getStorageTypeFromConfig(config: Config): StorageType.StorageType = {
    config.getString(STORAGE_TYPE_CONFIG_KEY) match {
      case null =>
        val v = getStorageDflt
        getLogger.warn(s"Storage backend not defined in application.conf. Using 'storage' = '$v'.")
        v

      case t =>
        val t1 = t.toUpperCase
        val result = StorageType.withName(t1)
        getLogger.info(s"$result selected as storage backend for majority of models according to application.conf.")
        result
    }
  }

}


import SioModelUtil._

trait StorageTypeFromConfigT {

  protected def getConfig: Config

  protected def getStorageTypeFromConfig: StorageType = {
    SioModelUtil getStorageTypeFromConfig getConfig
  }

  /** Выбор хранилища, которую будут использовать модели. */
  val STORAGE: StorageType = getStorageTypeFromConfig

}


