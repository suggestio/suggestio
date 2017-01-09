package models.adv.geo

import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.MItemStatus
import io.suggest.mbill2.m.item.typ.MItemType
import org.joda.time.Interval

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.12.16 19:12
  * Description:
  */
package object cur {

  /** Тип сырых данных по шейпу, приходит из базы в таком вот виде. */
  type AdvGeoShapeInfo_t = (Option[String], Option[Gid_t], Option[Boolean])

  /** Выхлоп отчёта по размещения в указанной гео-области. */
  type AdvGeoBasicInfo_t = (Gid_t, MItemType, MItemStatus, Option[Interval], Option[String])

}
