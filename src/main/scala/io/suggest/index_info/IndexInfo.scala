package io.suggest.index_info

import org.joda.time.LocalDate
import io.suggest.model.{SioSearchContext, JsonDfsBackend}
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.TimeValue
import scala.concurrent.Future
import org.elasticsearch.action.search.SearchResponse
import io.suggest.util.SioEsUtil._
import io.suggest.util.Logs
import org.elasticsearch.action.admin.indices.optimize.OptimizeRequestBuilder
import scala.concurrent.ExecutionContext.Implicits.global
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.04.13 16:09
 * Description: Интерфейсы и константы для классов *IndexInfo.
 */

trait IndexInfo extends Logs with Serializable {

  import IndexInfoStatic._

  val dkey: String

  // Вернуть алиас типа
  val iitype : IITYPE_t

  /**
   * Строка, которая дает идентификатор этому индексу в целом, безотносительно числа шард/типов и т.д.
   * @return ASCII-строка без пробелов, включающая в себя имя используемой шарды и, вероятно, dkey.
   */
  def name: String

  // имя индекса, в которой лежат страницы для указанной даты
  def indexTypeForDate(d:LocalDate) : (String, String)

  def export : IIMap_t

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

  def allShards: List[String]
  def allTypes:  List[String]
  def shardTypesMap : Map[String, Seq[String]]

  /**
   * Убедиться, что все шарды ES созданы.
   * @return true, если всё ок.
   */
  def ensureShards(implicit client:Client): Future[Boolean] = {
    Future.traverse(allShards) { shardName =>
      client
        .admin()
        .indices()
        .prepareCreate(shardName)
        .setSettings(getNewIndexSettings(shards=1))
        .execute()
        .map { _.isAcknowledged }

    } map iterableOnlyTrue
  }


  /**
   * Залить все маппинги в базу. Пока что тут у всех одинаковый маппинг, однако потом это всё будет усложняться и
   * выноситься на уровень имплементаций.
   * @return true, если всё ок.
   */
  def ensureMappings(implicit client:Client) : Future[Boolean] = {
    val adm = client.admin().indices()
    Future.traverse(shardTypesMap) { case (inx, types) =>
      Future.traverse(types) { typ =>
        adm.preparePutMapping(inx)
          .setSource(getPageMapping(typ))
          .execute()
          .map { resp =>
            val result = resp.isAcknowledged
            // При ошибке написать в лог
            if(!result) {
              error("Cannot ensure inx/typ %s/%s: %s" format(inx, typ, resp.getHeaders))
            }
            result
          }
      } map iterableOnlyTrue
    } map iterableOnlyTrue
  }

  def delete(implicit client: Client): Future[Boolean]


  /**
   * Удалить только данные из индексов, не трогая сами индексы. Это ресурсоемкая операция, используется для удаления
   * части данных из индекса.
   * @return true, если всё нормально.
   */
  def deleteMappings(implicit client: Client): Future[Boolean] = {
    Future.traverse(shardTypesMap) {
      case (inx, types) => deleteMappingsFrom(inx, types)
    } map iterableOnlyTrue
  }


  /**
   * Подготовиться к скроллингу по индексу. Скроллинг позволяет эффективно проходить по огромным объемам данных курсором.
   * @return scroll_id, который нужно передавать аргументом в сlient.prepareSearchScroll()
   */
  def startScrollAll(timeoutSec:Long = 60l, sizePerShard:Int = 25)(implicit client:Client): Future[SearchResponse] = {
    client
      .prepareSearch(allShards: _*)
      .setTypes(allTypes: _*)
      .setSize(sizePerShard)
      .setScroll(TimeValue.timeValueSeconds(timeoutSec))
      .execute()
  }

}


object IndexInfoStatic extends Logs with Serializable {

  // Описание идентификатор типов
  type IITYPE_t = String
  type IIMap_t  = JsonDfsBackend.JsonMap_t
  type AnyJsonMap = scala.collection.Map[String, Any]

  val IITYPE_SMALL_MULTI  : IITYPE_t = "smi"
  val IITYPE_BIG_SHARDED  : IITYPE_t = "bsi"

  /**
   * Импорт данных из экспортированной карты и типа.
   * @param iitype тип (выхлоп iitype)
   * @param iimap карта (результат IndexInfo.export)
   * @return Инстанс IndexInfo.
   */
  def apply(dkey:String, iitype: IITYPE_t, iimap: AnyJsonMap): IndexInfo = {
    iitype match {
      case IITYPE_SMALL_MULTI => SmallMultiIndex(dkey, iimap)
      case IITYPE_BIG_SHARDED => BigShardedIndex(dkey, iimap)
    }
  }

  val iterableOnlyTrue = {l: Iterable[Boolean] => l.forall(_ == true)}

  val futureTrue  = Future.successful(true)
  val futureFalse = Future.successful(false)


  /**
   * Удалить указанные типы только из указанного индекса.
   * @param inx имя шарды.
   * @param types Удаляемые типы.
   * @return true, если всё нормально.
   */
  def deleteMappingsFrom(inx:String, types:Seq[String], optimize:Boolean = true)(implicit client: Client): Future[Boolean] = {
    val adm = client.admin().indices()
    // TODO тут лучше бы последовательное, а не парралельное удаление...
    val fut = Future.traverse(types) { typ =>
      adm.prepareDeleteMapping(inx)
        .setType(typ)
        .execute()
        .map(_ => true)
    } .map(iterableOnlyTrue)
    // Заказать оптимизацию индекса, если удаление маппингов прошло ок.
    if (optimize) {
      fut.onSuccess { case true =>
        debug("Expunge deletes on index %s..." format inx)
        new OptimizeRequestBuilder(adm)
          .setIndices(inx)
          .setOnlyExpungeDeletes(true)
          .execute()
      }
    }
    // Вернуть фьючерс
    fut
  }

  /**
   * Удалить указанные шарды целиком.
   * @param indicies имя шарды
   * @return Фьючерс с ответом о завершении удаления индекса.
   */
  def deleteShard(indicies:String *)(implicit client:Client): Future[Boolean] = {
    info("Deleting empty index %s..." format indicies)
    val fut: Future[DeleteIndexResponse] = {
      client.admin().indices()
        .prepareDelete(indicies: _*)
        .execute()
    }
    // Если включен дебаг, то доложить в лог о завершении.
    if (logger.isDebugEnabled) {
      fut.onComplete { result =>
        debug("Index %s deletion result: %s" format(indicies, result))
      }
    }
    fut.map(_.isAcknowledged)
  }

}