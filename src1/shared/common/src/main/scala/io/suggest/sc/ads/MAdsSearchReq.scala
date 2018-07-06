package io.suggest.sc.ads

import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ad.search.AdSearchConstants._
import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.es.model.MEsUuId
import io.suggest.sc.search.MSearchTab
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.17 18:41
  * Description: Модель поиска карточек.
  * Реализована вместо одноимённой модели в sc, т.к. исходная модель была чисто-трейтовой,
  * что очень неудобно для play-json.
  */
object MAdsSearchReq extends IEmpty {

  override type T = MAdsSearchReq
  override def empty: T = apply()

  /** Поддержка play-json. */
  implicit def mAdsSearchResFormat: OFormat[MAdsSearchReq] = (
    (__ \ PRODUCER_ID_FN).formatNullable[MEsUuId] and
    (__ \ LIMIT_FN).formatNullable[Int] and
    (__ \ OFFSET_FN).formatNullable[Int] and
    (__ \ RECEIVER_ID_FN).formatNullable[MEsUuId] and
    (__ \ GENERATION_FN).formatNullable[Long] and
    (__ \ TAG_NODE_ID_FN).formatNullable[MEsUuId] and
    (__ \ TEXT_QUERY_FN).formatNullable[String] and
    (__ \ SEARCH_TAB_FN).formatNullable[MSearchTab]
    // Не забывать добавлять биндеры в MScQsJvm.
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MAdsSearchReq] = UnivEq.derive

}


case class MAdsSearchReq(
                          prodId      : Option[MEsUuId]       = None,
                          limit       : Option[Int]           = None,
                          offset      : Option[Int]           = None,
                          rcvrId      : Option[MEsUuId]       = None,
                          genOpt      : Option[Long]          = None,
                          tagNodeId   : Option[MEsUuId]       = None,
                          textQuery   : Option[String]        = None,
                          tab         : Option[MSearchTab]    = None,
                        )
  extends EmptyProduct
{

  def withOffset(offset: Option[Int] = None) = copy(offset = offset)
  def withLimit(limit: Option[Int] = None)   = copy(limit = limit)

  def withLimitOffset(limit: Option[Int], offset: Option[Int]) = copy(limit = limit, offset = offset)

}
