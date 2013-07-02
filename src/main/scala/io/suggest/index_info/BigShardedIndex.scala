package io.suggest.index_info

import com.github.nscala_time.time.Imports._
import io.suggest.util.{Logs, JacksonWrapper}
import org.joda.time
import io.suggest.model.SioSearchContext
import IndexInfoConstants._

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

  val iitype : IITYPE_T = IITYPE_BIG_SHARDED

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
  def export: Map[String, Any] = Map(
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

}


// Компаньон для конструирования BSI и других статических задач.
object BigShardedIndex extends Logs {

  /**
   * Собрать класс SII на базе домена и экспортированного состояния.
   * @param dkey ключ-домен
   * @param m экспортированное состояние в виде карты
   * @return
   */
  def apply(dkey:String, m:Map[String,Any]) : BigShardedIndex = {
    new BigShardedIndex(
      dkey = dkey,
      generation = m("generation").asInstanceOf[Int],
      // Тут имеет смысл запилить обработчик ошибок импорта
      shards = JacksonWrapper.convert[List[Shard]](m("shards"))
    )
  }

  def apply(dkey:String, indexName:String, pageType:String) = {
    new BigShardedIndex(
      dkey = dkey,
      shards = shardsForAddedDomain(dkey, indexName, pageType)
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


