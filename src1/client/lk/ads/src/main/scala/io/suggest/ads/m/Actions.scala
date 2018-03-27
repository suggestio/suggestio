package io.suggest.ads.m

import io.suggest.ads.MLkAdsOneAdResp
import io.suggest.spa.DAction

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 21:45
  * Description: Экшены формы управления карточками.
  */
sealed trait ILkAdsAction extends DAction


/** Скачать ещё карточки с сервера. */
case class GetMoreAds(clean: Boolean) extends ILkAdsAction

/** Результат запроса карточек с сервера. */
case class GetMoreAdsResp(reason: GetMoreAds, tryResp: Try[Seq[MLkAdsOneAdResp]]) extends ILkAdsAction

/** Экшен изменения состояния показа карточки в выдаче родительского узла. */
case class SetAdShownAtParent(adId: String, isShown: Boolean) extends ILkAdsAction
