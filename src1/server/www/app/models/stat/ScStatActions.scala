package models.stat

import enumeratum.values.{StringEnum, StringEnumEntry}
import japgolly.univeq.UnivEq

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.10.14 13:45
 * Description: Статистические данные по showcase для сохранения в MAdStat.
 */

object ScStatActions extends StringEnum[ScStatAction] {

  /** Просмотр плитки выдачи. */
  case object Tiles extends ScStatAction("v")

  /** Юзер открывает карточку, вызывая focusedAds() на сервере. */
  case object Opened extends ScStatAction("c")

  /** Запрос к "сайту" выдачи, т.е. к странице, с которой начинается рендер выдачи. */
  case object Site extends ScStatAction("s")

  /** Запрос к showcase/index, т.е. к верстке выдачи узла какого-то например. */
  case object Index extends ScStatAction("i")

  /** Запрос списка нод от клиента. */
  case object Nodes extends ScStatAction("n")


  override def values = findValues

  def onlyBillableIter: Iterator[ScStatAction] = {
    values
      .iterator
      .filter(_.billable)
  }

  def onlyBillable: Set[ScStatAction] = {
    onlyBillableIter.toSet
  }

}


/**
  * Экземпляр этой модели.
  * @param strId Строковой id.
  */
sealed abstract class ScStatAction(override val value: String) extends StringEnumEntry {
  /** @param billable Допускается для биллинга? */
  def i18nCode = "ad.stat.action." + value
}

object ScStatAction {

  @inline implicit def univEq: UnivEq[ScStatAction] = UnivEq.derive

  implicit class ScStatActionOpsExt( val ssa: ScStatAction ) extends AnyVal {

    def billable: Boolean = {
      ssa match {
        case ScStatActions.Tiles | ScStatActions.Opened => true
        case _ => false
      }
    }

  }

}
