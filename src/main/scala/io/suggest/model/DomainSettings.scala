package io.suggest.model

import com.github.nscala_time.time.Imports._
import org.joda.time.DateMidnight

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.04.13 14:50
 * Description: Модель работы с настройками домена. Настройки храняться в json-файлах в hdfs.
 */

object DomainSettings {

  // Описание идентификатор типов
  type IITYPE_T = Char

  // multiindex используется для микса из разных сайтов в одном индексе.
  // Это помогает сохранить оперативку и хранить данные по-компактнее.
  val IITYPE_MULTIINDEX : IITYPE_T = 'm'

  // Отдельные индексы используются для сайтов крупнее. Шардинг эмулируется тут же, ибо ES не может гарантировать
  // что одна страница будет лежать в конкретной шарде.
  val IITYPE_SEPARATE   : IITYPE_T = 's'

  // TODO: Гибридный индекс. Это мультииндекс, используемый для нескольких доменов одновременно. Это полезно, когда
  //       поиск на нескольких доменах объединен воедино.
  //val IITYPE_HYBRID     : IITYPE_T = 'h'


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
          case IITYPE_SEPARATE   => SeparateIndexInfo(dkey, iimap)
          case IITYPE_MULTIINDEX => MultiIndexInfo(dkey, iimap)
        }
      },
      meta = m("meta").asInstanceOf[Map[String,Any]]
    )
  }

  /**
   * Сборка нового состояния настроек.
   * @param dkey нормальное имя домена, т.е ключ домена во всей системе.
   * @return
   */
  def apply(dkey:String) : DomainSettings =
    apply(dkey, start_url = "http://" + dkey + "/")

  def apply(dkey:String, start_url:String) : DomainSettings =
    apply(dkey, start_url, index_info = new SeparateIndexInfo(dkey))

  def apply(dkey:String, start_url:String, index_info:IndexInfo) = {
    new DomainSettings(
      dkey       = dkey,
      start_url  = start_url,
      index_info = index_info
    )
  }


  protected def getName = getClass.getCanonicalName

  /**
   * Загрузить из хранилища данные для указанного домена
   * @param dkey ключ домена (не проверяется)
   * @return Объект DomainSettings, если такой был некогда сохранен.
   */
  def load(dkey:String) : Option[DomainSettings] = {
    JsonDfsBackend.getAs[Map[String,Any]](dkey, getName) map(apply(_))
  }

}


// Объект, хранящий все настройки для домена.
case class DomainSettings(
  dkey : String,
  index_info : IndexInfo,
  start_url : String,
  // Сериализуемое хранилище метаданных типа ключ-значение. Изначально хранит только дату добавления сайта в миллисекундах.
  meta : Map[String, Any] = Map(
    "domain_added_date_utc" -> DateTime.now.toInstant.getMillis
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
  def save() {
    val state = export
    JsonDfsBackend.writeTo(
      dkey = dkey,
      name = getClass.getCanonicalName,
      value = state
    )
  }
}

trait IndexInfo {

  // Вернуть алиас типа
  def iitype : DomainSettings.IITYPE_T

  // имя индекса, в которой лежат страницы для указанной даты
  def i4date(d:LocalDate) : String

  def export : Map[String, Any]

  // тип, используемый для хранения страниц.
  val type_page : String
}


// Separate Index - значит сайт живёт в своём собственном индексе или множестве индексов-шард.
case class SeparateIndexInfo(
  dkey : String,
  // Поколение индексов. При ребилде индекса в новый si это значение инкрементится:
  generation : Int = 0,
  // Номер индекса и нижняя дата (включительно). В порядке возрастания номера и убывания даты:
  shards : List[(Int, LocalDate)] = List(0 -> LocalDate.now)

) extends IndexInfo {

  def iitype : DomainSettings.IITYPE_T = DomainSettings.IITYPE_SEPARATE

  protected val last_id = shards.last._1

  /**
   * Узнать индекс для указанной даты. Используется при сохранении страницы.
   * Нужно найти шарду во множестве шард, сравнивая указанную дату с точками дат.
   * @param d Дата без времени.
   * @return Название индекса, к которому относится указанная дата.
   */
  def i4date(d:LocalDate): String = {
    val shard_id = shards.find { case (n, d1) => d <= d1 }.map { _._1 } getOrElse(last_id)
    dkey + "." + generation + "." + shard_id
  }

  /**
   * Тип, используемый для хранения индексированных страниц. Для всех один.
   * @return имя типа.
   */
  val type_page: String = "p"


  // Экспорт состояния в карту
  def export: Map[String, Any] = Map(
    "generation" -> generation,
    "shards"     -> shards.map { case (id, d) => (id, d.toDateMidnight.toInstant.getMillis) }.toMap
  )
}

object SeparateIndexInfo {
  def apply(dkey:String, m:Map[String,Any]) : SeparateIndexInfo = {
    new SeparateIndexInfo(
      dkey = dkey,
      generation = m("generation").asInstanceOf[Int],
      shards = m("shards").asInstanceOf[Map[Int,Long]].map {
        case (id, instantMs) => (id, new DateMidnight(instantMs).toLocalDate)
      }.toList
    )
  }
}



// Мульти-индекс используется для группировки индексов маленьких сайтов.
case class MultiIndexInfo(
  dkey: String,
  mi_id : Int
) extends IndexInfo {

  def iitype : DomainSettings.IITYPE_T = DomainSettings.IITYPE_MULTIINDEX

  protected val _mi = "mi_" + mi_id

  /**
   * Индекс для указанной даты. Используется при сохранении страницы.
   * @param d Дата без времени.
   * @return Название индекса, к которому относится указанная дата.
   */
  def i4date(d:LocalDate): String = _mi


  /**
   * Тип, используемый для хранения страниц. Индексированных страниц.
   * В мульти-индексах для разделения сайтов используются разные типы.
   * @return
   */
  val type_page: String = dkey + "$p"


  /**
   * Экспорт состояния энтерпрайза.
   * @return
   */
  def export: Map[String, Any] = Map(
    "mi_id" -> mi_id
  )
}

// Компаньон для импорта данных в MultiIndexInfo
object MultiIndexInfo {

  def apply(dkey:String, m:Map[String,Any]) : MultiIndexInfo = {
    new MultiIndexInfo(dkey, m("mi_id").asInstanceOf[Int])
  }

}

