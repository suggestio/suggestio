package io.suggest.adn.mapf

import io.suggest.maps.nodes.MRcvrsMapUrlArgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.17 16:32
  * Description: Модель данных конфигурации Lam-формы.
  */
case class MLamConf(
                     nodeId       : String,
                     rcvrsMap     : MRcvrsMapUrlArgs,
                   )
