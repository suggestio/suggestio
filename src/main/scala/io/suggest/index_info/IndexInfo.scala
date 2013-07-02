package io.suggest.index_info

import org.joda.time.LocalDate
import io.suggest.model.SioSearchContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.04.13 16:09
 * Description: Интерфейсы и константы для классов *IndexInfo.
 */

trait IndexInfo {

  val dkey: String

  // Вернуть алиас типа
  val iitype : IndexInfoConstants.IITYPE_T

  /**
   * Строка, которая дает идентификатор этому индексу в целом, безотносительно числа шард/типов и т.д.
   * @return ASCII-строка без пробелов, включающая в себя имя используемой шарды и, вероятно, dkey.
   */
  def name: String

  // имя индекса, в которой лежат страницы для указанной даты
  def indexTypeForDate(d:LocalDate) : (String, String)

  def export : Map[String, Any]

  // тип, используемый для хранения страниц.
  def type_page : String

  /**
   * Является ли индекс шардовым или нет? Генератор реквестов может учитывать это при построении
   * запроса к ElasticSearch.
   */
  def isSharded : Boolean

  /**
   * Вебморда собирается сделать запрос, и ей нужно узнать то, какие индесы необходимо опрашивать
   * и какие типы в них фильтровать.
   * TODO тут должен быть некий экземпляр request context, который будет описывать ход поиска.
   * @param sc Контекст поискового запроса.
   * @return Список названий индексов-шард и список имен типов в этих индексах.
   */
  def indexesTypesForRequest(sc:SioSearchContext) : (List[String], List[String])

}


object IndexInfoConstants extends Serializable {

  // Описание идентификатор типов
  type IITYPE_T = String

  val IITYPE_SMALL_MULTI  : IITYPE_T = "smi"
  val IITYPE_BIG_SHARDED  : IITYPE_T = "bsi"

}