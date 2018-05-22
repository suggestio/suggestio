package io.suggest.sc.ads

import io.suggest.dev.MScreen
import io.suggest.geo.MLocEnv
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ad.search.AdSearchConstants._
import io.suggest.common.empty.EmptyUtil
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.17 18:41
  * Description: Модель поиска карточек.
  * Реализована вместо одноимённой модели в sc, т.к. исходная модель была чисто-трейтовой,
  * что очень неудобно для play-json.
  */
object MFindAdsReq {

  /** Поддержка сериализации в play-json. MScreen пока не поддерживает ДЕсериализацию. */
  implicit def MFIND_ADS_REQ_WRITES: OWrites[MFindAdsReq] = (
    (__ \ PRODUCER_ID_FN).writeNullable[String] and
    (__ \ LIMIT_FN).writeNullable[Int] and
    (__ \ OFFSET_FN).writeNullable[Int] and
    (__ \ RECEIVER_ID_FN).writeNullable[String] and
    (__ \ GENERATION_FN).writeNullable[Long] and
    (__ \ LOC_ENV_FN).writeNullable[MLocEnv]
      .contramap[MLocEnv]( EmptyUtil.implEmpty2OptF ) and
    (__ \ SCREEN_INFO_FN).writeNullable[MScreen] and
    (__ \ TAG_NODE_ID_FN).writeNullable[String] and
    (__ \ FOC_JUMP_ALLOWED_FN).writeNullable[Boolean] and
    (__ \ AD_LOOKUP_MODE_FN).writeNullable[MLookupMode] and
    (__ \ AD_ID_LOOKUP_FN).writeNullable[String]
  )( unlift(unapply) )

  implicit def univEq: UnivEq[MFindAdsReq] = UnivEq.derive

}


// TODO После выпиливания старых выдачи, надо отрефакторить модель, распихав поля по под-моделями


case class MFindAdsReq(
                        // search API
                        producerId  : Option[String]    = None,
                        limit       : Option[Int]       = None,
                        offset      : Option[Int]       = None,
                        receiverId  : Option[String]    = None,
                        generation  : Option[Long]      = None,
                        locEnv      : MLocEnv           = MLocEnv.empty,
                        screen      : Option[MScreen]   = None,
                        tagNodeId   : Option[String]    = None,
                        // focused API
                        /**
                          * Флаг, сообщающий серверу о допустимости возврата index-ответа или
                          * иного переходного ответа вместо focused json.
                          */
                        allowReturnJump   : Option[Boolean]       = None,
                        /** Задание режима lookup'а карточек. */
                        adsLookupMode     : Option[MLookupMode]   = None,
                        /** id базовой рекламной карточки, относительно которой необходимо искать сегмент. */
                        adIdLookup        : Option[String]        = None,
                      ) {

  def withOffset(offset: Option[Int] = None) = copy(offset = offset)
  def withLimit(limit: Option[Int] = None)   = copy(limit = limit)

  def withLimitOffset(limit: Option[Int], offset: Option[Int]) = copy(limit = limit, offset = offset)

}
