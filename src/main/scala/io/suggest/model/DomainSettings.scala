package io.suggest.model

import com.github.nscala_time.time.Imports._
import org.apache.hadoop.fs.FileSystem
import io.suggest.index_info._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.04.13 14:50
 * Description: Модель работы с настройками домена. Настройки храняться в json-файлах в hdfs.
 */

object DomainSettings extends IndexInfoConstants {

  /**
   * Сборка настроек из сохраненного ранее состояния
   * @param m карта экспортированного состояния.
   * @return Готовый объект DomainSettings.
   */
  def apply(m:Map[String, Any]) : DomainSettings = {
    val dkey = m("dkey").asInstanceOf[String]
    new DomainSettings(
      dkey = dkey,
      start_url = m("start_url").asInstanceOf[String],
      index_info = {
        val iimap = m("index_info").asInstanceOf[Map[String,Any]]
        m("index_info_type").asInstanceOf[IITYPE_T] match {
          case IITYPE_BIG_SHARDED => BigShardedIndex(dkey, iimap)
          case IITYPE_SMALL_MULTI => SmallMultiIndex(dkey, iimap)
        }
      },
      meta = m("meta").asInstanceOf[Map[String,Any]]
    )
  }

  /**
   * Был добавлен новый домен в базу. Тут создается дефолтовое состояние для начальных параметров.
   * @param dkey нормальное имя домена, т.е ключ домена во всей системе.
   * @return
   */
  def apply(dkey:String, indexName:String, pageType:String) : DomainSettings = {
    new DomainSettings(
      dkey = dkey,
      start_url = "http://" + dkey + "/",
      index_info = BigShardedIndex(dkey, indexName, pageType)
    )
  }

  protected def getName = getClass.getCanonicalName

  /**
   * Загрузить из хранилища данные для указанного домена
   * @param dkey ключ домена (не проверяется)
   * @return Объект DomainSettings, если такой был некогда сохранен.
   */
  def load(dkey:String)(implicit fs:FileSystem) : Option[DomainSettings] = {
    JsonDfsBackend.getAs[Map[String,Any]](dkey, getName, fs) map(apply(_))
  }

}


// Объект, хранящий все настройки для домена.
case class DomainSettings(
  dkey : String,
  index_info : IndexInfo,
  start_url : String,
  // Сериализуемое хранилище метаданных типа ключ-значение. Изначально хранит только дату добавления сайта в миллисекундах.
  meta : Map[String, Any] = Map(
    "domain_added_date_utc" -> DateTime.now.withZone(DateTimeZone.UTC).toInstant.getMillis
  )
) {

  /**
   * Экспорт всего состояния в легко сериализуемый объект, состящий из стандартных простых типов.
   * @return Map(key:String -> something)
   */
  def export : Map[String, Any] = {
    Map(
      "dkey"            -> dkey,
      "start_url"       -> start_url,
      "index_info_type" -> index_info.iitype,
      "index_info"      -> index_info.export,
      "meta"            -> meta
    )
  }

  /**
   * Вызвать экспорт в json и сохранение награбленного.
   */
  def save(implicit fs:FileSystem) : DomainSettings = {
    val state = export
    JsonDfsBackend.writeTo(
      dkey = dkey,
      name = getClass.getCanonicalName,
      value = state
    )
    this
  }
}

