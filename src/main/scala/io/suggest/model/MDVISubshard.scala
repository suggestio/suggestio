package io.suggest.model

import scala.concurrent.{ExecutionContext, Future}
import io.suggest.util.{LogsImpl, SioEsIndexUtil}
import org.elasticsearch.client.Client
import scala.util.{Failure, Success}
import org.joda.time.LocalDate
import io.suggest.util.DateParseUtil.toDaysCount
import cascading.tuple.Tuple
import scala.collection.JavaConversions._
import cascading.tuple.coerce.Coercions.INTEGER

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.07.13 17:08
 * Description: Виртуальная подшарда для индекса. Используется для группировки индексированных страниц во времени.
 * Управление номерами шард сделано для возможных случаев, когда необходимо или же наоборот нежелательно
 * партициирование того или иного типа. Например, в целях экономии RAM при индексировании данных, для которых
 * нежелательно партициирование данных из-за неустранимого long-tail в текстовом индексе (куча редких слов).
 */
object MDVISubshard {

  /**
   * Сериализовать значение .subshardInfo в кортеж.
   * @param lsi Исходный список MDVISubshardInfo.
   * @param t Аккамулятор. Если не задан, то будет использован новый кортеж.
   * @return Обновлённый аккамулятор.
   */
  def serializeSubshardInfo(lsi: List[MDVISubshardInfo], t:Tuple = new Tuple): Tuple = {
    lsi.foreach { si =>
      t.add(si.lowerDateDays)
      val ts: Tuple = if (si.shards.isEmpty) {
        null
      } else {
        new Tuple(si.shards.map(INTEGER.coerce) : _*)
      }
      t.add(ts)
    }
    t
  }


  /** Десериализовать subshardInfo
    * @param sii Исходный список, полученный из сериализованного кортежа.
    * @return
    */
  def deserializeSubshardInfo(sii: List[AnyRef]): List[MDVISubshardInfo] = {
    if (sii.size % 2 != 0) {
      throw new IllegalArgumentException("invalid size of serialized subshardsInfo.")
    }
    val (None, si) = sii.foldLeft[(Option[Int], List[MDVISubshardInfo])] (None -> Nil) {
      // Нечетный шаг. Текущий элемент - это сериализованный lowerDateDays.
      case ((None, acc), ldd) =>
        val lddInt = INTEGER.coerce(ldd).intValue()
        Some(lddInt) -> acc

      // Четный шаг. Текущий элемент - сериализованный список id шард mvi.
      case ((Some(ldd), acc), ts) =>
        val shardsIds = if (ts == null) {
          Nil
        } else {
          ts.asInstanceOf[Tuple].toList.map { INTEGER.coerce(_).intValue }
        }
        val acc1 = MDVISubshardInfo(ldd, shardsIds) :: acc
        None -> acc1
    }
    si.reverse
  }

  /** Десериализовать subshard info их котржеа.
   * @param sii Tuple
   * @return Список MDVISubshard.
   */
  def deserializeSubshardInfo(sii: Tuple): List[MDVISubshardInfo] = deserializeSubshardInfo(sii.toList)


  /** Определать id es-шарды для указазанного времени.
    * @param shardIds Список шард, из которых надо выбрать.
    * @param daysCount кол-во дней, результат функции DateParseUtil.toDaysCount().
    * @return shard id, один из shards.
    */
  def getShardId(shardIds:Seq[Int], daysCount:Int): Int = {
    // TODO хреновый алгоритм какой-то тут
    val daysCount1 = Math.max(1000, daysCount)
    val shardNumber = daysCount1 % shardIds.size
    val shardId = shardIds(shardNumber)
    shardId
  }

}


case class MDVISubshard(
  dvin:         MDVIActive,
  subshardData: MDVISubshardInfo
) extends MDVISubshardInfoT {

  private val LOGGER = new LogsImpl(getClass)
  import LOGGER._

  import subshardData._

  protected val logPrefix: String = dvin.toShortString + "-" + lowerDateDays

  /**
   * Получить список названий шард на основе их id'шников.
   */
  lazy val getShards: Seq[String] = {
    if (shards.isEmpty) {
      dvin.getShards
    } else {
      val vin = dvin.vin
      shards.map(MVirtualIndex.esShardNameFor(vin, _))
    }
  }


  /** Получить кол-во задействованных шард. */
  def getUsedShards: Seq[Int] = if (shards.isEmpty) {
    dvin.getVirtualIndex.getShardIds
  } else {
    shards
  }


  /**
   * Вернуть тип, который будет адресоваться в рамках ES.
   * @return Строка типа индекса. Например "suggest.io-123123".
   */
  def getTypename: String = subshardData.getTypename(dvin.dkey)


  /**
   * Выдать шарду для указанной даты. Если мультишардовый тип, вычисляется через hashCode mod shardCount.
   * @param date дата, к которой относится страница.
   * @return Имя индекса
   */
  def getShardForDate(date: LocalDate): String = getShardForDaysCount(toDaysCount(date))


  /** Выдать значение lowerDateDays для текущей подшарды. */
  def lowerDateDays = subshardData.lowerDateDays
  def lowerDate     = subshardData.lowerDate
  def shardIds      = subshardData.shards


  /**
   * Выдать es-шарду для указанного days count.
   * @param daysCount кол-во дней от начала эпохи.
   * @return Название реальной шарды.
   */
  def getShardForDaysCount(daysCount: Int): String = {
    val shardIds = getUsedShards
    val shardId = MDVISubshard.getShardId(shardIds, daysCount)
    //trace(s"getShardForDaysCount(dc=$daysCount): shardId=$shardId shardCount=${shardIds.size} subshardData=$subshardData")
    MVirtualIndex.esShardNameFor(dvin.vin, shardId)
  }


  /** Выдать id'шники ES-шард. */
  def getShardIds: Seq[Int] = if (shards.isEmpty) {
    dvin.getVirtualIndex.getShardIds
  } else {
    shards
  }


  /**
   * Выставить маппинг в es для всех индексов.
   * @param indices Список индексов. По дефолту запрашивается у виртуального индекса.
   * @param failOnError Сдыхать при ошибке. По дефолту true.
   * @return фьючерс с isAcknowledged.
   */
  def setMappings(indices:Seq[String] = getShards, failOnError:Boolean = true)(implicit client:Client, executor:ExecutionContext): Future[Boolean] = {
    val _typename = getTypename
    val fut = SioEsIndexUtil.setMappingsFor(indices, _typename, failOnError)
    fut onComplete { case result =>
      lazy val msgPrefix = s"setMappings(indices=${indices.mkString(",")} failOnError=$failOnError) getTypename=${_typename} => "
      result match {
        case Success(true)  => debug(msgPrefix + "successed")
        case Failure(ex)    => error(msgPrefix + "failed", ex)
        case Success(false) => warn(msgPrefix + "finished with isAck == false!")
      }
    }
    fut
  }


  /**
   * Удалить маппинги (вероятно, содержащие данные), созданные из setMappings.
   * @param indices индексы. По дефолту взять из файла данных.
   * @return Выполненный фьючерс, если всё нормально.
   */
  def deleteMappaings(indices: Seq[String] = getShards)(implicit client:Client, executor:ExecutionContext): Future[Unit] = {
    val _typename = getTypename
    debug("deleteMappings(%s) getTypename=%s" format (indices, _typename))
    SioEsIndexUtil.deleteMappingsSeqFrom(indices, Seq(_typename))
  }

}


