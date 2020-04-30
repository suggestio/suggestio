package io.suggest.kv

import io.suggest.msg.ErrorMsgs
import io.suggest.log.Log
import japgolly.univeq.UnivEq
import org.scalajs.dom
import play.api.libs.json.{Json, Reads, Writes}

import scala.scalajs.js

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
        logger.error(ErrorMsgs.KV_STORAGE_CHECK_FAILED, ex)
        false
    }
  }


  /** Сохранение в базу.
    */
  def save[V: Writes](mkv: MKvStorage[V]): Unit = {
    val vSerial = Json
      .toJson(mkv.value)
      .toString()
    storage.setItem(mkv.key, vSerial )
  }


  /** Чтение из модели.
    *
    * @param key Ключ.
    * @return Опциональный результат.
    */
  def get[V: Reads](key: String): Option[MKvStorage[V]] = {
    for {
      v <- Option(storage.getItem(key))
    } yield {
      MKvStorage(key, Json.parse(v).as[V] )
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

  @inline implicit def univEq[V: UnivEq]: UnivEq[MKvStorage[V]] = UnivEq.derive

}


/** Данные для модели конфига.
  *
  * @param key Ключ.
  * @param value Значение.
  * @tparam V Тип значения
  */
case class MKvStorage[V](
                          key    : String,
                          value  : V,
                        )
