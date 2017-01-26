package models.adv.geo.cur

import java.time.OffsetDateTime

import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.MItemStatus
import io.suggest.mbill2.m.item.typ.MItemType

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.12.16 22:01
  * Description: Модель надстройка над сырыми кортежами базовой инфы по георазмещениям.
  */
object MAdvGeoBasicInfo {

  /** Собрать на базе кортежа от slick'а. */
  def apply(t: AdvGeoBasicInfo_t): MAdvGeoBasicInfo = {
    val (id, iType, status, dtStartOpt, dtEndOpt, tagFaceOpt) = t
    apply(
      id          = id,
      iType       = iType,
      status      = status,
      dtStartOpt  = dtStartOpt,
      dtEndOpt    = dtEndOpt,
      tagFaceOpt  = tagFaceOpt
    )
  }

}

case class MAdvGeoBasicInfo(
                             id           : Gid_t,
                             iType        : MItemType,
                             status       : MItemStatus,
                             dtStartOpt   : Option[OffsetDateTime],
                             dtEndOpt     : Option[OffsetDateTime],
                             tagFaceOpt   : Option[String]
                           )
