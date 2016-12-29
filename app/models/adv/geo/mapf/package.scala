package models.adv.geo

import io.suggest.mbill2.m.gid.Gid_t

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.12.16 15:54
  */
package object mapf {

  /** Тип сырых данных по шейпу, приходит из базы в таком вот виде. */
  type AdvGeoShapeInfo_t = (Option[String], Option[Gid_t], Option[Boolean])

}
