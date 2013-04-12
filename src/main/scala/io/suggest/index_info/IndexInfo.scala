package io.suggest.index_info

import org.joda.time.LocalDate
import io.suggest.model.SioSearchContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.04.13 16:09
 * Description: Интерфейсы и константы для классов *IndexInfo.
 */

trait IndexInfo extends IndexInfoConstants {

  // Вернуть алиас типа
  def iitype : IITYPE_T

  // имя индекса, в которой лежат страницы для указанной даты
  def indexTypeForDate(d:LocalDate) : (String, String)

  def export : Map[String, Any]

  // тип, используемый для хранения страниц.
  val type_page : String

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


trait IndexInfoConstants extends Serializable {

  // Описание идентификатор типов
  protected type IITYPE_T = String

  protected val IITYPE_SMALL_MULTI  : IITYPE_T = "small_multi"
  protected val IITYPE_BIG_SHARDED  : IITYPE_T = "big_sharded"

}