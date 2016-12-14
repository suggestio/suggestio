package models.adv.geo.mapf

import models.MNodeType

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.12.16 22:07
  * Description: Данные по ресиверу, забинденные в форме гео-размещения.
  * Модель биндится при сабмите формы.
  */
case class MRcvrBindedInfo(
  from      : String,
  to        : String,
  groupId   : Option[MNodeType],
  value     : Boolean
)
