package io.suggest.sc.ads

import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ad.search.AdSearchConstants._
import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.es.model.MEsUuId
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

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
    (__ \ TEXT_QUERY_FN).formatNullable[String]
    // Не забывать добавлять биндеры в MScQsJvm.
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MAdsSearchReq] = UnivEq.derive

  def rcvrId = GenLens[MAdsSearchReq](_.rcvrId)
  def genOpt = GenLens[MAdsSearchReq](_.genOpt)
  def offset = GenLens[MAdsSearchReq](_.offset)
  def prodId = GenLens[MAdsSearchReq](_.prodId)


  implicit class AsrOpsExt( val req: MAdsSearchReq ) extends AnyVal {

    def withOffset(offset: Option[Int] = None) =
      MAdsSearchReq.offset.set(offset)(req)

    def withLimitOffset(limit: Option[Int], offset: Option[Int]) =
      req.copy(limit = limit, offset = offset)

  }

}


case class MAdsSearchReq(
                          prodId      : Option[MEsUuId]       = None,
                          limit       : Option[Int]           = None,
                          offset      : Option[Int]           = None,
                          rcvrId      : Option[MEsUuId]       = None,
                          genOpt      : Option[Long]          = None,
                          tagNodeId   : Option[MEsUuId]       = None,
                          textQuery   : Option[String]        = None,
                        )
  extends EmptyProduct

