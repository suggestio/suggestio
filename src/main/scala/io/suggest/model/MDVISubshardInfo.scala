package io.suggest.model

import io.suggest.util.DateParseUtil
import org.joda.time.LocalDate

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

}


/** Трейт-интерфейс, описывающий общий функционал MDVISubshardInfo и MDVISubshard. */
trait MDVISubshardInfoT {
  def lowerDate: LocalDate
  def lowerDateDays: Int
}

