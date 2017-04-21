package models.mpay.yaka

import io.suggest.bill.{Amount_t, IPrice, MCurrencies, MCurrency}
import io.suggest.es.model.MEsUuId
import io.suggest.mbill2.m.gid.Gid_t

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.02.17 18:51
  * Description: Модель данных формы яндекс-кассы.
  */
case class MYakaFormData(
                          profile         : IYakaProfile,
                          amount          : Amount_t,
                          customerNumber  : String,
                          onNodeId        : MEsUuId,
                          orderNumber     : Option[Gid_t],
                          clientEmail     : Option[String]
                        )
  extends IPrice
{

  override def currency: MCurrency = MCurrencies.RUB

}
