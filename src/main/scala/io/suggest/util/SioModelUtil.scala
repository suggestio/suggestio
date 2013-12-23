package io.suggest.util

import com.typesafe.config.Config
import org.apache.hadoop.hbase.io.ImmutableBytesWritable

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.12.13 14:19
 * Description: Утиль для моделей s.io.
*/

// Модели могут переключаться между HBase, так и JsonDFS. Для продакшена подоходит только HBase, но не очень подходит
// для локалхоста. Переключение между hbase и fs происходит через конфиг, а модели при запуске выбирают свои backend'ы.
object StorageType extends Enumeration {
  type StorageType = Value
  val HBASE, DFS = Value
}

import StorageType._

object SioModelUtil {

  /** Ключ в конфигах, значение которого описывает используемый backend в моделях. */
  val STORAGE_TYPE_CONFIG_KEY = "storage"

  private def getLogger = new LogsImpl(getClass)
  private def getStorageDflt = DFS

  def getStorageTypeFromConfig(config: Config): StorageType.StorageType = {
    MyConfig(config).getString(STORAGE_TYPE_CONFIG_KEY) match {
      case None =>
        val v = getStorageDflt
        getLogger.warn(s"Storage backend not defined in application.conf. Using 'storage' = '$v'.")
        v

      case Some(t) =>
        val t1 = t.toUpperCase
        val result = StorageType.withName(t1)
        getLogger.info(s"$result selected as storage backend for majority of models according to application.conf.")
        result
    }
  }


  /** Десериализация строки, использованной в качестве одной из координат ячейки в таблице HBase (или др.). */
  val deserialzeHCellCoord: PartialFunction[AnyRef, String] = {
    case ar: Array[Byte]              =>  deserialzeHCellCoord(new String(ar))
    case str: String                  =>  str
    case ibw: ImmutableBytesWritable  =>  deserialzeHCellCoord(ibw.get)
  }

  /** Сериализовать строку для использования в качестве ключа ряда, CFки или qualifier'а, которые будут
    * десериализованы с помощью deserialzeHCellCoord().
   * @param s Исходная строка.
   * @return Байты, пригодные для использования в качестве значения.
   */
  def serializeStrForHCellCoord(s: String) = s.getBytes

  /**
   * Сериализовать dkey для использования dkey в качестве ключа ряда HTable.
   * Сортировка в HBase-таблицах идёт по ключу, а поддомены должны лежать рядом. Поэтому эта функция используется
   * правильной сериализации dkey.
   * @param dkey Ключ домена.
   * @return Байты, пригодные для значения rowkey.
   */
  def dkey2rowkey(dkey: String): Array[Byte] = {
    serializeStrForHCellCoord(
      UrlUtil.reverseDomain(dkey))
  }

  /**
   * Десериализовать результат dkey2rowkey() обратно в dkey.
   * @param rowkey Ключ ряда, сгенеренный в dkey2rowkey()
   * @return dkey, т.е. исходный ключ домена.
   */
  def rowkey2dkey(rowkey: AnyRef): String = {
    val dkeyRev = deserialzeHCellCoord(rowkey)
    UrlUtil.reverseDomain(dkeyRev)
  }

}


object SioDefaultStorage extends StorageTypeFromConfigT {
  protected def getConfig: Config = MyConfig.CONFIG.underlying
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


