package io.suggest.ads.m

import io.suggest.spa.DAction

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
case class GetModeAdsResp(reason: GetMoreAds) extends ILkAdsAction
