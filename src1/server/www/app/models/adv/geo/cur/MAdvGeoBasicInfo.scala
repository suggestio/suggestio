package models.adv.geo.cur

import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.MItemStatus
import io.suggest.mbill2.m.item.typ.MItemType
import org.joda.time.Interval

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.12.16 22:01
  * Description: Модель надстройка над сырыми кортежами базовой инфы по георазмещениям.
  */
object MAdvGeoBasicInfo {

  /** Собрать на базе кортежа от slick'а. */
  def apply(t: AdvGeoBasicInfo_t): MAdvGeoBasicInfo = {
    val (id, iType, status, intervalOpt, tagFaceOpt) = t
    apply(
      id          = id,
      iType       = iType,
      status      = status,
      intervalOpt = intervalOpt,
      tagFaceOpt  = tagFaceOpt
    )
  }

}

case class MAdvGeoBasicInfo(
                             id           : Gid_t,
                             iType        : MItemType,
                             status       : MItemStatus,
                             intervalOpt  : Option[Interval],
                             tagFaceOpt   : Option[String]
                           )
