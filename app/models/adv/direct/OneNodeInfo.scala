package models.adv.direct

import io.suggest.model.sc.common.AdShowLevels

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.15 16:24
 * Description: Промежуточная модель для данных маппинга.
 */
trait IOneNodeInfo {
  def adnId : String
  def isAdv : Boolean

  // 2016.feb.12 Удаление нерабочих уровней отображения и их поддержки привели вот к этому.
  def sls    = Set(AdShowLevels.LVL_START_PAGE)
}

case class OneNodeInfo(
  override val adnId : String,
  override val isAdv : Boolean
)
  extends IOneNodeInfo
