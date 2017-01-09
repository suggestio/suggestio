package models.stat

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.10.14 13:45
 * Description: Статистические данные по showcase для сохранения в MAdStat.
 */

object ScStatActions extends Enumeration {

  /**
   * Экземпляр этой модели.
   * @param strId Строковой id.
   * @param billable Допускается для биллинга?
   */
  protected class Val(val strId: String, val billable: Boolean) extends super.Val(strId) {
    def i18nCode = "ad.stat.action." + strId
  }


  type ScStatAction = Val


  /** Просмотр плитки выдачи. */
  val Tiles: ScStatAction  = new Val("v", billable = true)

  /** Юзер открывает карточку, вызывая focusedAds() на сервере. */
  val Opened: ScStatAction = new Val("c", billable = true)

  /** Запрос к "сайту" выдачи, т.е. к странице, с которой начинается рендер выдачи. */
  val Site: ScStatAction  = new Val("s", billable = false)

  /** Запрос к showcase/index, т.е. к верстке выдачи узла какого-то например. */
  val Index: ScStatAction = new Val("i", billable = false)

  /** Запрос списка нод от клиента. */
  val Nodes: ScStatAction = new Val("n", billable = false)


  implicit def value2val(x: Value): ScStatAction = x.asInstanceOf[ScStatAction]

  def onlyBillableIter: Iterator[ScStatAction] = {
    values
      .iterator
      .filter(_.billable)
      .map(value2val)
  }

  def onlyBillable: Set[ScStatAction] = {
    onlyBillableIter.toSet
  }

}

