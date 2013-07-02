package io.suggest.index_info

import com.github.nscala_time.time.Imports._
import io.suggest.model.SioSearchContext
import IndexInfoConstants._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.04.13 16:15
 * Description: Если сайт (домен сайта) слишком мал, чтобы держать для этого отдельный индекс, то
 * индексы таких сайт объединяются в "мультииндекс", т.е. индекс, где используется принцип "один сайт -> один тип".
 */

case class SmallMultiIndex(
  dkey: String,
  mi_id : Int
) extends IndexInfo {

  val iitype : IITYPE_T = IITYPE_SMALL_MULTI

  protected val _mi = "mi" + mi_id


  /**
   * Строка, которая дает идентификатор этому индексу в целом, безотносительно числа шард/типов и т.д.
   * @return ASCII-строка без пробелов, включающая в себя имя используемой шарды и, вероятно, dkey.
   */
  lazy val name: String = _mi + "." + type_page

  /**
   * Индекс+тип для указанной даты. Используется при сохранении страницы.
   * @param d Дата без времени.
   * @return Название индекса, к которому относится указанная дата.
   */
  def indexTypeForDate(d:LocalDate): (String, String) = (_mi, type_page)


  /**
   * Вебморда собирается сделать запрос, и ей нужно узнать то, какие индесы необходимо опрашивать
   * и какие типы в них фильтровать.
   * @param sc Контекст поиска.
   * @return Список названий индексов-шард и список имен типов в этих индексах.
   */
  def indexesTypesForRequest(sc:SioSearchContext): (List[String], List[String]) = {
    (List(_mi), List(type_page))
  }

  /**
   * Тип, используемый для хранения страниц. Индексированных страниц.
   * В мульти-индексах для разделения сайтов используются разные типы.
   * @return dkey, ибо адресация в мультииндексе происходит по ключу домена.
   */
  val type_page: String = dkey


  /**
   * Экспорт состояния энтерпрайза.
   * @return
   */
  def export: Map[String, Any] = Map(
    "mi_id" -> mi_id
  )


  /**
   * Мини-индекс не является многошардовым.
   * @return
   */
  def isSharded : Boolean = false

}


// Компаньон для импорта данных в MultiIndexInfo
object SmallMultiIndex {

  /**
   * Собрать класс мультииндекса на основе домена и экспортированного состояния.
   * @param dkey ключ домена
   * @param m карта экспортированного состояния
   * @return MultiIndexInfo
   */
  def apply(dkey:String, m:Map[String,Any]) : SmallMultiIndex = {
    new SmallMultiIndex(dkey, m("mi_id").asInstanceOf[Int])
  }

}
