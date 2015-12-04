package util.adv.gtag

import models.MNode
import models.adv.gtag.MAdvFormResult

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.15 13:43
 * Description: Утиль для биллинга размещений в тегах.
 */
class GeoTagAdvBillUtil {

  /** Закинуть в корзину bill-v2  */
  def addToCart(mnode: MNode, res: MAdvFormResult): Future[_] = {
    mnode.billing.contractId
    ???
  }

}
