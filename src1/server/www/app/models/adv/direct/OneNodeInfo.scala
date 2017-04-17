package models.adv.direct

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.15 16:24
 * Description: Промежуточная модель для данных маппинга.
 */
trait IOneNodeInfo {
  def adnId : String
  def isAdv : Boolean
}

case class OneNodeInfo(
  override val adnId : String,
  override val isAdv : Boolean
)
  extends IOneNodeInfo
