package models.adv.bill

import io.suggest.common.menum.EnumMaybeWithName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.11.15 15:40
 * Description: Модель режимов/состояний экземпляров модели [[MAdv2]].
 */

object MAdv2Modes extends EnumMaybeWithName {

  /** Класс для всех экземпляров модели. */
  protected[this] abstract class Val(val strId: String)
    extends super.Val(strId)
  {
    /** Суффикс названия adv2-таблицы. */
    def tableSuffix: String

    /** Полное имя таблицы, связанной с данным типом. */
    def tableName: String = {
      MAdv2.TABLE_NAME + "_" + tableSuffix
    }
  }

  override type T = Val

  /** Запрос на размещение, пока не прошедший модерацию. */
  val Req: T = new Val("r") {
    override def tableSuffix = "req"
  }

  /** Размещение подтверждено, но пока ещё время не пришло для активации. */
  val Approved: T = new Val("a") {
    override def tableSuffix = "approved"
  }

  /** Размещение в процессе. */
  val Online: T = new Val("o") {
    override def tableSuffix = "online"
  }

  /** Все завершенные размещения лежат в одной таблице done. */
  private class DoneVal(strId: String) extends Val(strId) {
    override def tableSuffix = "done"
  }

  /** Отказ в размещении. */
  val Rejected: T = new DoneVal("e")

  /** Размещение было выполнено, и закончено затем. */
  val Finished: T = new DoneVal("f")

}
