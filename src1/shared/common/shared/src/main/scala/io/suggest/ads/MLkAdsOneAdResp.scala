package io.suggest.ads

import io.suggest.common.empty.EmptyUtil
import io.suggest.jd.MJdData
import io.suggest.mbill2.m.item.status.MItemStatus
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.03.18 17:31
  * Description: Модель-контейнер данных по одной карточке в GetAds-ответе сервера.
  *
  * Сам GetAds-ответ сервера -- это js array в chunked, а тут модель одного элемента массива.
  */
object MLkAdsOneAdResp {

  /** Поддержка play-json. */
  implicit def MLK_ADS_ONE_AD_RESP: OFormat[MLkAdsOneAdResp] = (
    (__ \ "s").formatNullable[Set[MItemStatus]]
      .inmap[Set[MItemStatus]](
        EmptyUtil.opt2ImplEmpty1F(Set.empty),
        { statuses => if (statuses.isEmpty) None else Some(statuses) }
      ) and
    (__ \ "j").format[MJdData] and
    (__ \ "p").format[Boolean]
  )(apply, unlift(unapply))


  @inline implicit def univEq: UnivEq[MLkAdsOneAdResp] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

  def shownAtParent = GenLens[MLkAdsOneAdResp](_.shownAtParent)

}


/** Инфа по одной карточке.
  *
  * @param advStatuses Статусы текущих размещений.
  * @param jdAdData Отрендеренная карточка.
  * @param shownAtParent Активно ли сейчас размещение на продьюсере?
  */
case class MLkAdsOneAdResp(
                            advStatuses   : Set[MItemStatus],
                            jdAdData      : MJdData,
                            shownAtParent : Boolean
                          )

