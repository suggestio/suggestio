package models.adv.direct

import models.adv.form.{IAdvFormResult, MDatesPeriod}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.15 16:25
 * Description: Результат маппинга формы adv direct.
 */
case class FormResult(
  nodes                   : List[OneNodeInfo]   = Nil,
  override val period     : MDatesPeriod        = MDatesPeriod()
)
  extends IAdvFormResult
{

  def nodeIdsIter = nodes.iterator.map(_.adnId)

}
