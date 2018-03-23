package io.suggest.ads

import play.api.libs.json.OFormat

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 22:25
  * Description: Модел ответа сервера на запрос карточек.
  */
object MGetAdsResp {

  implicit def MGET_ADS_RESP_FORMAT: OFormat[MGetAdsResp] = {
    ???
  }

}

case class MGetAdsResp(
                      // TODO список отрендеренных блоков-карточек + мета-инфа.
                      )
