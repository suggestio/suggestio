package io.suggest.kv

import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.log.Log
import org.scalajs.dom

import scala.scalajs.js
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.18 14:27
  * Description: JS-модель хранения данных конфигурации или иных строковых данных.
  */
object MKvStorage extends Log {

  /** Доступ к хранилищу. */
  def storage = dom.window.localStorage


  /** Проверить доступность модели. */
  def isAvailable: Boolean = {
    try {
      !js.isUndefined( storage )
    } catch {
      case ex: Throwable =>
        LOG.error(ErrorMsgs.KV_STORAGE_CHECK_FAILED, ex)
        false
    }
  }


  /** Сохранение в базу.
    *
    * @param mconf
    */
  def save(mkv: MKvStorage): Unit = {
    storage.setItem(mkv.key, mkv.value)
  }


  /** Чтение из модели.
    *
    * @param key Ключ.
    * @return Опциональный результат.
    */
  def get(key: String): Option[MKvStorage] = {
    for {
      v <- Option(storage.getItem(key))
    } yield {
      MKvStorage(key, v)
    }
  }


  /** Удаление по ключу.
    *
    * @param key Ключ.
    */
  def delete(key: String): Unit = {
    storage.removeItem(key)
  }


  /** Очистка данных. */
  def deleteAll(): Unit = {
    storage.clear()
  }

}


/** Данные для модели конфига.
  *
  * @param key Ключ.
  * @param value Значение.
  */
case class MKvStorage(
                       key    : String,
                       value  : String,
                     ) {

  def withValue(value: String) = copy(value = value)

}
