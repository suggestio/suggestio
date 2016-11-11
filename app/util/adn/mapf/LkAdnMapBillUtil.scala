package util.adn.mapf

import com.google.inject.Inject
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.status.MItemStatus
import models.madn.mapf.MAdnMapFormRes
import models.mproj.ICommonDi

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.11.16 22:12
  * Description: Биллинг размещения узлов просто на карте.
  */
class LkAdnMapBillUtil @Inject() (
  protected val mCommonDi   : ICommonDi
) {

  import mCommonDi._
  import slick.driver.api._


  /**
    * Добавление item'а размещения ADN-узла на карте в ордер (корзину).
    *
    * @param orderId id ордера-корзины.
    * @param nodeId id ADN-узла, размещаемого на карте.
    * @param formRes Результат биндинга формы размещения.
    * @param status Статус новых item'ов.
    * @return DB-экшен добавления заказа в ордер.
    */
  def addToOrder(orderId: Gid_t, nodeId: String, formRes: MAdnMapFormRes, status: MItemStatus): DBIOAction[Seq[MItem], NoStream, Effect.Write] = {
    // TODO Запилить логику добавления item'а в ордер.
    // TODO Подумать на тему максимум одной покупки и отката других adn-map размещений после оплаты.
    ???
  }

}
