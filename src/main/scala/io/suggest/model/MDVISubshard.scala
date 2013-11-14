package io.suggest.model

import scala.concurrent.{ExecutionContext, Future}
import io.suggest.util.{DateParseUtil, LogsPrefixed, SioEsIndexUtil}
import org.elasticsearch.client.Client
import io.suggest.index_info.MDVIUnit
import scala.util.{Failure, Success}
import org.joda.time.LocalDate
import io.suggest.util.DateParseUtil.toDaysCount
import cascading.tuple.{Tuples, Tuple}
import scala.collection.JavaConversions._

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
        new Tuple(si.shards.map(Tuples.toIntegerObject) : _*)
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
        val lddInt = Tuples.toInteger(ldd)
        Some(lddInt) -> acc

      // Четный шаг. Текущий элемент - сериализованный список id шард mvi.
      case ((Some(ldd), acc), ts) =>
        val shardsIds = if (ts == null) {
          Nil
        } else {
          ts.asInstanceOf[Tuple].toList.map(Tuples.toInteger)
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

}


case class MDVISubshard(
  dvin:         MDVIUnit,
  subshardData: MDVISubshardInfo
) extends LogsPrefixed {

  import subshardData._

  protected val logPrefix: String = dvin.id + "-" + lowerDateDays

  /**
   * Получить список названий шард на основе их id'шников.
   */
  lazy val getShards: Seq[String] = {
    if (shards.isEmpty) {
      dvin.getShards
    } else {
      val vin = dvin.vin
      shards.map(MVirtualIndex.vinAndCounter2indexName(vin, _))
    }
  }

  /**
   * Получить кол-во шард прямо из мультииндекса.
   */
  def getShardsCount: Int = dvin.getVirtualIndex.shardCount


  /**
   * Вернуть тип, который будет адресоваться в рамках ES.
   * @return Строка типа индекса. Например "suggest.io-123123".
   */
  def getTypename: String = dvin.dkey + "-" + lowerDateDays


  /**
   * Выдать шарду для указанной даты. Если мультишардовый тип, вычисляется через hashCode mod shardCount.
   * @param date дата, к которой относится страница.
   * @return Имя индекса
   */
  def getShardForDate(date:LocalDate): String = getShardForDaysCount(toDaysCount(date))


  /** Выдать значение lowerDateDays для текущей подшарды. */
  def lowerDateDays = subshardData.lowerDateDays
  def lowerDate     = subshardData.lowerDate
  def shardIds      = subshardData.shards


  /**
   * Выдать шарду для указанного days count.
   * @param daysCount кол-во дней от начала эпохи.
   * @return Название реальной шарды.
   */
  def getShardForDaysCount(daysCount:Int): String = {
    val shardNumber = daysCount % getShardsCount
    MVirtualIndex.vinAndCounter2indexName(dvin.vin, shardNumber)
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
    fut andThen { case result =>
      val msgPrefix = "setMappings(indices=%s failOnError=%s) getTypename=%s " format (indices, failOnError, _typename)
      result match {
        case Success(true)  => debug(msgPrefix + "successed")
        case Failure(ex)    => error(msgPrefix + "failed", ex)
        case Success(false) => warn(msgPrefix + "finished with isAck = false!")
      }
    }
  }


  /**
   * Удалить маппинги (вероятно, содержащие данные), созданные из setMappings.
   * @param indices индексы. По дефолту взять из файла данных.
   * @return Выполненный фьючерс, если всё нормально.
   */
  def deleteMappaings(indices:Seq[String] = getShards)(implicit client:Client, executor:ExecutionContext): Future[Unit] = {
    val _typename = getTypename
    debug("deleteMappings(%s) getTypename=%s" format (indices, _typename))
    SioEsIndexUtil.deleteMappingsSeqFrom(indices, Seq(_typename))
  }

}


/**
 * Сами данные по шарде вынесены за скобки.
 * @param lowerDateDays Нижняя дата этой подшарды в днях.
 * @param shards Номера задействованных шард в vin. Если Nil, то значит нужно опрашивать
 *               всю родительскую виртуальную шарду.
 */
case class MDVISubshardInfo(
  lowerDateDays: Int,
  shards:        List[Int] = Nil
) extends Serializable {

  /** Представление lowerDateDays в виде даты. */
  def lowerDate = DateParseUtil.dateFromDaysCount(lowerDateDays)
}

