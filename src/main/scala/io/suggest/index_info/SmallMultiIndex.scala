package io.suggest.index_info

import com.github.nscala_time.time.Imports._
import io.suggest.model.SioSearchContext
import IndexInfoStatic._
import org.elasticsearch.client.Client
import scala.concurrent.Future
import io.suggest.util.SioEsUtil._
import org.elasticsearch.action.admin.indices.optimize.OptimizeRequestBuilder
import org.elasticsearch.action.count.CountRequestBuilder
import scala.concurrent.ExecutionContext.global

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

  val iitype : IITYPE_t = IITYPE_SMALL_MULTI

  protected val _mi = "mi" + mi_id

  /**
   * Тип, используемый для хранения страниц. Индексированных страниц.
   * В мульти-индексах для разделения сайтов используются разные типы.
   * @return dkey, ибо адресация в мультииндексе происходит по ключу домена.
   */
  val type_page: String = dkey

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


  lazy val allShards = List(_mi)
  lazy val allTypes  = List(type_page)
  lazy val shardTypesMap: Map[String, Seq[String]] = Map(_mi, Seq(type_page))

  /**
   * Удаление индекса из кластера ES. Для мультииндекса -- это удаление типа из мультииндекса, т.е. удаление маппинга типа
   * и последующая оптимизация индекса.
   * // TODO возможно, следует удалить мультииндекс, если он пуст?
   * @return true, если данные больше не существуют и поверхностная оптимизация индекса завершена.
   */
  def delete(implicit client: Client): Future[Boolean] = {
    val fut = deleteOnlyData

    // В фоне: если индекс теперь пуст, то его надо бы удалить.
    fut onSuccess { case true =>
      // TODO возможно есть более эффективный метод, без полного подсчета? Через статистику индекса, например...
      new CountRequestBuilder(client).setIndices(_mi).execute().foreach { countResp =>
        val c = countResp.getCount
        debug("Index %s contains %s docs." format(_mi, c))
        if (c == 0) {
          // Удаляем пустой индекс
          info("Deleting empty index %s..." format _mi)
          client.admin().indices().prepareDelete(_mi).execute()
        }
      }
    }

    // И вернуть фьючерс будущего результата.
    fut
  }

}


// Компаньон для импорта данных в MultiIndexInfo
object SmallMultiIndex {

  /**
   * Собрать класс мультииндекса на основе домена и экспортированного состояния.
   * @param dkey ключ домена
   * @param m карта экспортированного состояния
   * @return MultiIndexInfo
   */
  def apply(dkey:String, m:AnyJsonMap) : SmallMultiIndex = {
    val mi_id = m("mi_id").asInstanceOf[Int]
    new SmallMultiIndex(dkey, mi_id)
  }

}
