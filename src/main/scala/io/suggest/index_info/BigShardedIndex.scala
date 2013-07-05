package io.suggest.index_info

import com.github.nscala_time.time.Imports._
import io.suggest.util.{Logs, JacksonWrapper}
import org.joda.time
import io.suggest.model.SioSearchContext
import IndexInfoStatic._
import scala.concurrent.ExecutionContext
import org.elasticsearch.client.Client
import scala.concurrent.Future
import io.suggest.util.SioEsUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.04.13 16:13
 * Description: Если сайт большой, то он шардится на большие части, а внутри частей происходит субшардинг для дробления
 * по диапазонам дат с помощью особого именования типов. Считается, что бОльшая часть содержимого индекса относится
 * к одному домену и, возможно, его поддоменам или родственным, очень близким, доменам.
 */

case class BigShardedIndex(
  dkey : String,
  // Номер индекса и нижняя дата (включительно). В порядке возрастания номера и убывания даты:
  shards : List[Shard],
  // Поколение индексов. При ребилде индекса в новый si это значение инкрементится:
  generation : Int = 0

) extends IndexInfo {

  val iitype : IITYPE_t = IITYPE_BIG_SHARDED

  protected val isSingleShard = shards.tail == Nil

  /**
   * Строка, которая дает идентификатор этому индексу в целом, безотносительно числа шард/типов и т.д.
   * @return ASCII-строка без пробелов, включающая в себя имя используемой шарды и, вероятно, dkey.
   */
  lazy val name: String = dkey + "." + shards.length

  /**
   * Узнать индекс+тип для указанной даты. Используется при сохранении страницы в индекс.
   * Нужно найти шарду во множестве шард, в которой лежат страницы в диапазоне, который относится к указанной дате.
   * @param d Дата без времени.
   * @return Индекс и тип, в который надо сохранять страницу.
   */
  def indexTypeForDate(d: time.LocalDate): (String, String) = {
    val days = BigShardedIndex.toDaysCount(d)
    // Ищем шарду, удовлетворяющую дате
    val shard = isSingleShard match {
      case true  => shards.head
      case false => shards find { _.lowerDate < days } getOrElse shards.last
    }
    // В выбранной шарде ищем тип, удовлетворяющий дате
    val subshards = shard.subshards
    val typ = if (subshards.tail == Nil) {
      subshards.head
    } else {
      // Подобрать тип по дате
      subshards find { _.lowerDate < days } getOrElse subshards.last
    }
    (shard.indexName, typ.typeName)
  }


  /**
   * Вебморда собирается сделать запрос, и ей нужно узнать то, какие индесы необходимо опрашивать
   * и какие типы в них фильтровать.
   * @param sc контекст поиска
   * @return Список названий индексов-шард и список имен типов в этих индексах.
   */
  def indexesTypesForRequest(sc:SioSearchContext): (List[String], List[String]) = {
    // TODO: STUB!!! придумать и запилить логику поиска тут. Сейчас просто выдается первая шарда целиком.
    val shard = shards.head
    (List(shard.indexName), shard.subshards.map(_.typeName))
  }


  /**
   * Тип, используемый для хранения индексированных страниц. Для всех один.
   * @return имя типа.
   */
  val type_page: String = dkey


  // Экспорт состояния в карту, пригодную для сериализации в json
  def export = Map(
    "generation" -> generation,
    "shards"     -> shards
  )


  /**
   * Индекс является многошардовым? Да, если в работе используется больше одного индекса или
   * больше одного типа в единственном индексе.
   * @return
   */
  def isSharded : Boolean = {
    if (isSingleShard)
      shards.head.subshards.tail == Nil
    else
      true
  }

  /**
   * Выдать список всех индексов.
   * @return Список названий es-индексов в порядке убывания даты.
   */
  def allShards: List[String] = {
    shards.map { _.indexName }
  }

  /**
   * Выдать список всех задействованных типов во всех индексах.
   * @return Список типов (суб-шард) во всех шардах в порядке убывания дат.
   */
  def allTypes: List[String] = {
    shards.flatMap { _.subshards.map { _.typeName } }
  }


  /**
   * Выдать es-карту индекса.
   * @return
   */
  def shardTypesMap: Map[String, Seq[String]] = shards.map { shard =>
    shard.indexName -> shard.subshards.map(_.typeName)
  } toMap


  /**
   * Асинхронное удаление данных из ES. Нужно удалить все шарды.
   * Если в одном индексе лежат несколько доменов сразу, то проверять список типов (список маппингов) и сверяется с текущим.
   * @return true, если всё ок.
   */
  def delete(implicit client: Client, executor:ExecutionContext): Future[Boolean] = {
    val shardNames = shards.map(_.indexName)
    // Читаем маппинги http://elasticsearch-users.115913.n3.nabble.com/Java-API-to-get-a-mapping-td2988351.html
    client.admin().cluster().prepareState().setFilterIndices(shardNames: _*).execute().flatMap { resp =>
      val cmd = resp.getState.getMetaData
      val hasOtherShards = shardTypesMap.groupBy { case (inx, types) =>
        val imd = cmd.index(inx)
        val currMappings = imd.getMappings
        currMappings.size > types.length
        // TODO может стоит сверять типы?
      }
      // Начать удаление данных из индексов.
      Future.traverse(hasOtherShards) {
        // Эти индексы можно снести целиком
        case (true, m) =>
          deleteShard(m.keys.toSeq: _*)

        // Нельзя удалить эти индексы целиком. Запустить парралельное удаление маппингов во всех занятых индексах.
        case (false, m) =>
          Future
            .traverse(m) { case (inx, types) => deleteMappingsFrom(inx, types) }
            .map(iterableOnlyTrue)

      } map iterableOnlyTrue
    }
  }
}


// Компаньон для конструирования BSI и других статических задач.
object BigShardedIndex extends Logs {

  /**
   * Собрать класс SII на базе домена и экспортированного состояния.
   * @param dkey ключ-домен
   * @param m экспортированное состояние в виде карты
   * @return
   */
  def apply(dkey:String, m:AnyJsonMap) : BigShardedIndex = {
    new BigShardedIndex(
      dkey = dkey,
      generation = m("generation").asInstanceOf[Int],
      // Тут имеет смысл запилить обработчик ошибок импорта
      shards = JacksonWrapper.convert[List[Shard]](m("shards"))
    )
  }

  /**
   * Создать BSI на основе имени индекса и ключа домена. Используется при добавлении сайта.
   * @param dkey ключ домена.
   * @param indexName имя индекса
   * @param pageType Опциональное имя типа для проиндексированных страниц.
   * @return Экземпляр BSI.
   */
  def applyDomainAdd(dkey:String, indexName:String, pageType: Option[String] = None): BigShardedIndex = {
    new BigShardedIndex(
      dkey   = dkey,
      shards = shardsForAddedDomain(dkey, indexName, pageType = pageType getOrElse dkey)
    )
  }

  // Дефолтовая дата. Использовать, когда не знаешь чего за дату написать для нижней границы.
  val lowestDateDays = toDaysCount(LocalDate.now.minusYears(15))

  // Дефолтовая карта шард.
  def shardsForAddedDomain(dkey:String, indexName:String, pageType:String) = {
    val subshard = Subshard(dkey, lowestDateDays)
    val shard = Shard(indexName, lowestDateDays, List(subshard))
    List(shard)
  }


  /**
   * Приблизительное число дней от начала времён. Тут не требуется работать с миллисекундами,
   * поэтому точность в несколько дней достаточна.
   * @param d исходная дата, которую необходимо перевести в дни.
   * @return
   */
  def toDaysCount(d:LocalDate) : Int = {
    val year = d.getYear - 1980
    d.getDayOfYear + year * 365 + year/4 - year/100 + year/400
  }

}


final case class Shard(
  indexName : String,
  lowerDate : Int,
  subshards : List[Subshard]
)

final case class Subshard(
  typeName : String,
  lowerDate : Int
)


