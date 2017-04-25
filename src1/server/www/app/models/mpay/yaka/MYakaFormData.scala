package models.mpay.yaka

import io.suggest.bill.IPrice
import io.suggest.es.model.MEsUuId
import io.suggest.mbill2.m.gid.Gid_t

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.02.17 18:51
  * Description: Модель данных формы яндекс-кассы.
  * @param orderPrice Стоимость заказа.
  * @param minPayPrice Минимальная стоимость к оплате, которая и выставляется к оплате.
  */
case class MYakaFormData(
                          profile         : IYakaProfile,
                          orderPrice      : IPrice,
                          minPayPrice     : Option[IPrice],
                          customerNumber  : String,
                          onNodeId        : MEsUuId,
                          orderNumber     : Option[Gid_t],
                          clientEmail     : Option[String]
                        )
{

  def payPrice: IPrice = {
    minPayPrice.getOrElse( orderPrice )
  }

}
