package io.suggest.model

import com.github.nscala_time.time.Imports._
import org.apache.hadoop.fs.FileSystem
import io.suggest.util.SiobixFs.fs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.04.13 14:50
 * Description: Модель работы с настройками домена. Настройки храняться в json-файлах в hdfs.
 */

object DomainSettings extends DomainSettingsStaticT

// Функции объекта DS вынесены в трайт, чтобы можно было экстендить функционал на стороне пользователй sioutil.
trait DomainSettingsStaticT {

  // Тип карты, которая хранит настройки. Value имеет динамический тип, и может содержать в себе DSMap_t.
  type DSMap_t = JsonDfsBackend.JsonMap_t

  val getName = classOf[DomainSettings].getCanonicalName

  /**
   * Собрать пустые дефолтовые настройки для [нового] указанного домена.
   * @param dkey ключ домена. Используется для сохранения настроек.
   * @return Экземпляр DomainSettingsT (не сохраненный).
   */
  def apply(dkey:String): DomainSettingsT = DomainSettingsEmpty(dkey)

  /**
   * Прочитать настройки для домена.
   * @param dkey ключ домена.
   * @return Опциональный экземпляр прочитанных настроек для домена. Если ничего не сохранено или домена нет, то будет None.
   */
  def getForDkey(dkey:String): Option[DomainSettingsT] = {
    JsonDfsBackend.getAs[DomainSettings](dkey=dkey, name=getName, fs=fs)
  }

  /**
   * Выдать хоть что-нибудь в качестве настроек для домена.
   * @param dkey ключ домена.
   * @return Всегда какой-то экземпляр DomainSettingsT, даже если домен не существует, настройки не сохранены или нечитаемы.
   */
  def getAnyForDkey(dkey:String): DomainSettingsT = {
    getForDkey(dkey) getOrElse apply(dkey)
  }

}


import DomainSettings.DSMap_t

trait DomainSettingsT {
  // Ключ домена. Используется, чтобы знать, куда сохранять данные.
  val dkey: String

  // Карта метаданных, которая легко сериализуется в JSON.
  def meta: DSMap_t

  /**
   * Фунцкия экспорта состояния.
   * @return Карту, пригодную для сериализации в JSON.
   */
  def export: DSMap_t = Map(
    "dkey" -> dkey,
    "meta" -> meta
  )

  /**
   * Сохранить в dfs.
   * @param fs dfs
   * @return Текущий экземпляр класса, хотя теоретически в будущем может возвращаться и новый экземпляр класса.
   */
  def save(implicit fs:FileSystem): DomainSettingsT = {
    JsonDfsBackend.writeTo(
      dkey  = dkey,
      name  = DomainSettings.getName,
      value = export
    )
    this
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

