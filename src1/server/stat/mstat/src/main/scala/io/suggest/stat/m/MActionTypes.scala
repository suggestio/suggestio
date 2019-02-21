package io.suggest.stat.m

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.16 17:49
  * Description: Типы экшенов, затрагиваемых статистикой.
  */
object MActionTypes extends StringEnum[MActionType] {

  /** Экшен логгирования данных текущего юзера. */
  case object Person extends MActionType("юзер")

  /** Обращение к /sc/site */
  case object ScSite extends MActionType("выдача: сайт")

  /** Для логгирования значения pov_adn_id. */
  case object PovNode extends MActionType("точка зрения узла")

  /** Обращение к /sc/index вывело на узел-ресивер. */
  case object ScIndexRcvr extends MActionType("выдача ресивера")

  /** /sc/index не нашел ресивера, но подобрал или сочинил узел с подходящим к ситуации оформлением. */
  case object ScIndexCovering extends MActionType("выдача с оформлением от узла")

  /** /sc/error/occurred/in/showcase Сабмит диагностики на клиенте. */
  case object ScRemoteDiag extends MActionType("выдача: диагностика на клиенте")

  case object ScRcvrAds extends MActionType("выдача: карточки ресивера")

  case object ScProdAds extends MActionType("выдача: карточки продьюсера")

  case object ScAdsTile extends MActionType("выдача: плитка")

  case object SearchLimit extends MActionType("поиск: лимит")

  case object SearchOffset extends MActionType("поиск: сдвиг")

  case object ScAdsFocused extends MActionType("выдача: focused-карточки")

  case object ScAdsFocusingOnAd extends MActionType("выдача: фокусировка на карточке")

  /** Маячок BLE слышен где-то рядом на расстоянии, указанном в сантиметрах в count. */
  case object BleBeaconNear extends MActionType("BLE-маячок, см")

  case object ScTags extends MActionType("выдача: теги")


  case object PayCheck extends MActionType("pay: check")

  case object PayBadReq extends MActionType("pay: bad req")

  case object PayBadOrder extends MActionType("pay: bad order")

  /** Почему-то объем оплаченных средств расходится с оплачиваемым заказом. */
  case object PayBadBalance extends MActionType("pay: bad balance")

  case object UnexpectedCookies extends MActionType("cookies: unexpected")

  case object Success extends MActionType("успех")

  case object Violation extends MActionType("нарушение")


  override def values = findValues

}


sealed abstract class MActionType(override val value: String) extends StringEnumEntry


object MActionType {

  implicit def mActionTypeFormat: Format[MActionType] =
    EnumeratumUtil.valueEnumEntryFormat( MActionTypes )

  @inline implicit def univEq: UnivEq[MActionType] = UnivEq.derive

}
