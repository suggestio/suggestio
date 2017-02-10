package models.mpay.yaka

import io.suggest.bill.Amount_t
import io.suggest.mbill2.m.gid.Gid_t

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.02.17 18:51
  * Description: Модель данных формы яндекс-кассы.
  */
case class MYakaFormData(
                          shopId          : Long,
                          scId            : Long,
                          sumRub          : Amount_t,
                          customerNumber  : String,
                          orderNumber     : Option[Gid_t],
                          clientEmail     : Option[String]
                        )
