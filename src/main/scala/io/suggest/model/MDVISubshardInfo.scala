package io.suggest.model

import io.suggest.util.DateParseUtil
import org.joda.time.LocalDate
import cascading.tuple.Tuple
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.11.13 15:39
 * Description: Легковесный элемент MDVIActive, описывающий контейнер в виртуальном индексе.
 */

object MDVISubshardInfo {

  /** Сгенерить имя типа. */
  def getTypename(dkey:String, lowerDateDays:Long): String = {
    dkey + "-" + lowerDateDays
  }

  /** Сериализация в кортеж, пригодный как для сериализации в байты, так и для переправки по flow. */
  def serializeToTuple(dsi: MDVISubshardInfo): Tuple = {
    val t = new Tuple
    t addInteger dsi.lowerDateDays
    val shardsSer: Tuple = if (dsi.shards.isEmpty) {
      null
    } else {
      val shardsTuple = new Tuple
      dsi.shards.foreach(shardsTuple.addInteger)
      shardsTuple
    }
    t add shardsSer
    t
  }


  /** Десериализация версии 1. Версия сериализованности хранится снаружи, где-то на уровне MDVIActive. */
  val deserializeV1: PartialFunction[AnyRef, MDVISubshardInfo] = {
    case t: Tuple =>
      val ldd = t getInteger 0
      val shards: List[Int] = t getObject 1 match {
        case null => Nil
        case shardsTuple: Tuple =>
          (0 until shardsTuple.size) map { i =>
            shardsTuple getInteger i
          } toList
      }
      MDVISubshardInfo(ldd, shards)
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
) extends MDVISubshardInfoT with Serializable {

  if (shards.sorted != shards) {
    throw new IllegalArgumentException("shard ids must be a sorted list!")
  }

  /** Представление lowerDateDays в виде даты. */
  def lowerDate = DateParseUtil.dateFromDaysCount(lowerDateDays)

  /** Сгенерить имя типа. */
  def getTypename(dkey: String) = MDVISubshardInfo.getTypename(dkey, lowerDateDays)

  def serializerToTuple = MDVISubshardInfo.serializeToTuple(this)
}


/** Трейт-интерфейс, описывающий общий функционал MDVISubshardInfo и MDVISubshard. */
trait MDVISubshardInfoT {
  def lowerDate: LocalDate
  def lowerDateDays: Int
}

