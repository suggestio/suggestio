package io.suggest.model

import com.github.nscala_time.time.Imports._
import scala.concurrent.{Future, ExecutionContext}
import io.suggest.util.JacksonWrapper
import HTapConversionsBasic._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.04.13 14:50
 * Description: Модель работы с настройками домена. Настройки храняться в json-файлах в hdfs.
 */

object DomainSettings extends DomainSettingsStaticT {
 // Тип карты, которая хранит настройки. Value имеет динамический тип, и может содержать в себе DSMap_t.
 type DSMap_t = JsonDfsBackend.JsonMap_t

 val COLUMN = "_ds"
}

// Функции объекта DS вынесены в трайт, чтобы можно было экстендить функционал на стороне пользователй sioutil.
trait DomainSettingsStaticT {

  def COLUMN: String

  val getName = classOf[DomainSettings].getCanonicalName

  /**
   * Собрать пустые дефолтовые настройки для [нового] указанного домена.
   * @param dkey ключ домена. Используется для сохранения настроек.
   * @return Экземпляр DomainSettingsT (не сохраненный).
   */
  def empty(dkey: String): DomainSettingsT = DomainSettingsEmpty(dkey)

  /**
   * Прочитать настройки для домена.
   * @param dkey Ключ домена.
   * @return Опциональный экземпляр прочитанных настроек для домена. Если ничего не сохранено или домена нет, то будет None.
   */
  def getForDkey(dkey: String)(implicit ec: ExecutionContext): Future[Option[DomainSettingsT]] = {
    MObject.getProp(dkey, COLUMN) map {
      // Опциональный Array[Byte] маппим на опциональный экземпляр этой модели.
      _ map { v =>
        val meta = JacksonWrapper.deserialize[DomainSettings.DSMap_t](v)
        DomainSettings(dkey, meta)
      }
    }
  }

  /**
   * Выдать хоть что-нибудь в качестве настроек для домена.
   * @param dkey Ключ домена.
   * @return Всегда какой-то экземпляр DomainSettingsT, даже если домен не существует, настройки не сохранены или нечитаемы.
   */
  def getAnyForDkey(dkey:String)(implicit ec: ExecutionContext): Future[DomainSettingsT] = {
    getForDkey(dkey) map { _ getOrElse empty(dkey) }
  }

}


import DomainSettings.DSMap_t

trait DomainSettingsT extends Serializable {
  // Ключ домена. Используется, чтобы знать, куда сохранять данные.
  val dkey: String

  // Карта метаданных, которая легко сериализуется в JSON.
  def meta: DSMap_t

  /** Сохранить в хранилище.
   * @return Текущий экземпляр класса, хотя теоретически в будущем может возвращаться и новый экземпляр класса.
   */
  def save: Future[Unit] = {
    val v = JacksonWrapper.serialize(meta)
    MObject.setProp(dkey, DomainSettings.COLUMN, v)
  }

  // Короткие врапперы для изменения метаданных. Во всех случаях создаются новые инстансы неизменяемого DomainSettingsT.
  def +(kv: (String, Any)) = copy(meta + kv)
  def -(k: String)         = copy(meta - k)
  def ++(map1: DSMap_t)    = copy(meta ++ map1)

  def copy(newmeta:DSMap_t) = DomainSettings(dkey=dkey, meta=newmeta)
}


/**
 * Когда пустой набор настроек для домена. Тот тут просто инициалзация по сути.
 * @param dkey ключ домена.
 */
case class DomainSettingsEmpty(dkey:String) extends DomainSettingsT {
  val meta = Map(
    "domain_added_date_utc" -> DateTime.now.withZone(DateTimeZone.UTC)
  )
}

/**
 * Полноценные настройки, возможно уже сохраненные и считанные.
 * @param dkey ключ домена.
 * @param meta карта метаданных.
 */
case class DomainSettings(dkey:String, meta:DSMap_t) extends DomainSettingsT

