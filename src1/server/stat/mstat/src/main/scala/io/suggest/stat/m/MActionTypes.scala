package io.suggest.stat.m

import io.suggest.common.menum.{EnumMaybeWithName, StrIdValT}
import io.suggest.model.menum.EnumJsonReadsValT

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.16 17:49
  * Description: Типы экшенов, затрагиваемых статистикой.
  */
object MActionTypes extends EnumMaybeWithName with EnumJsonReadsValT with StrIdValT {

  /** Класс для всех экземпляров сий модели. */
  protected case class Val(override val strId: String)
    extends super.Val(strId)
    with ValT


  override type T = Val

  /** Экшен логгирования данных текущего юзера. */
  val CurrUser            = Val("юзер")

  /** Обращение к /sc/site */
  val ScSite              = Val("выдача: сайт")

  /** Для логгирования значения pov_adn_id. */
  val PovNode             = Val("точка зрения узла")

  /** Обращение к /sc/index вывело на узел-ресивер. */
  val ScIndexRcvr         = Val("выдача ресивера")

  /** /sc/index не нашел ресивера, но подобрал или сочинил узел с подходящим к ситуации оформлением. */
  val ScIndexCovering     = Val("выдача с оформлением от узла")

  /** /sc/error/occurred/in/showcase Сабмит диагностики на клиенте. */
  val ScRemoteDiag        = Val("выдача: диагностика на клиенте")

  val ScRcvrAds           = Val("выдача: карточки ресивера")

  val ScProdAds           = Val("выдача: карточки продьюсера")

  val ScAdsTile           = Val("выдача: плитка")

  val SearchLimit         = Val("поиск: лимит")

  val SearchOffset        = Val("поиск: сдвиг")

  val ScAdsFocused        = Val("выдача: focused-карточки")

  val ScAdsFocusingOnAd   = Val("выдача: фокусировка на карточке")

  /** Маячок BLE слышен где-то рядом на расстоянии, указанном в сантиметрах в count. */
  val BleBeaconNear       = Val("BLE-маячок, см")

  val ScTags              = Val("выдача: теги")


  val PayCheck            = Val("pay: check")

  val PayBadReq           = Val("pay: bad req")

  val PayBadOrder         = Val("pay: bad order")

  val UnexpectedCookies   = Val("cookies: unexpected")

  val Success             = Val("успех")

  val Person              = Val("юзер")

}
