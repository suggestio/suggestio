package io.suggest.pay

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.bill._
import io.suggest.common.empty.OptionUtil
import io.suggest.enum2.EnumeratumUtil
import io.suggest.i18n.MsgCodes
import io.suggest.sec.csp.CspPolicy
import io.suggest.url.bind.QsBindable
import japgolly.univeq._
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.04.17 11:55
  * Description: Enum-модель платёжных систем.
  */

/** Платёжные системы. */
object MPaySystems extends StringEnum[MPaySystem] {

  /** Yandex.Kassa. */
  case object YaKa extends MPaySystem("yaka") {

    override def supportedCurrency(currency: MCurrency): Option[ICurrencyPayInfo] = {
      // Поддерживаются только рубли.
      val rub = MCurrencies.RUB
      OptionUtil.maybe( currency ==* rub ) {
        val centsInRub = rub.centsInUnit
        MCurrencyPayInfo(
          currency              = rub,
          // Нижний лимит: по картам нельзя платить ниже 1 рубля. Поэтому считаем, что платить просто нельзя.
          lowerDebtLimitOpt     = Some( centsInRub ),
          // Верхний предел: взят с потолка.
          upperDebtLimitOpt     = Some( 1000000 * centsInRub )
        )
      }
    }

    /** id узла платежной системы.
      * Узел не существует, просто нужен был идентифицируемый id для статистики.
      */
    override def nodeIdOpt = Some( MsgCodes.`Yandex.Kassa` )

    override def nameI18n = MsgCodes.`Yandex.Kassa`

    /** У яндекс.кассы в продакшен режиме есть фреймы, которые не работают в связке с дефолтовым X-Frame-Options: DENY.
      * Поэтому, надо при возврате из этой ПС надо выставлять особое разрешение для хидера.
      * @return ВСЕГДА Some("...") с типом Some[String].
      */
    // 2016.may.12: Яндекс-касса уже починила фреймы 2 недели назад. Пока просто отключаем отработку фреймов тут:
    override def returnRespHdr_XFrameOptions_AllowFrom = None // Some( "https://money.yandex.ru/cashdesk/" )

    /** At most one external script is supported.
      * For possible multi-script loads in future, will need to implement support for parallel async+defer script loads.
      */
    override def payWidgetJsScriptUrl = None
  }


  /** YandexKassa transferred to Sberbank and renamed into "YooKassa" with new API v3. */
  case object YooKassa extends MPaySystem("yookassa") {

    override def supportedCurrency(currency: MCurrency): Option[ICurrencyPayInfo] = {
      // By now, only RUB is supported on
      val rub = MCurrencies.RUB
      OptionUtil.maybe( currency ==* rub ) {
        val centsInRub = rub.centsInUnit
        MCurrencyPayInfo(
          currency              = rub,
          // Lower limit: bank cards cannot pay above 1 RUB.
          lowerDebtLimitOpt     = Some( centsInRub ),
          // Upper limit: big random.
          upperDebtLimitOpt     = Some( 1000000 * centsInRub )
        )
      }
    }
    override def nameI18n = MsgCodes.`YooKassa`
    override def nodeIdOpt = Some( nameI18n )
    /** YooKassa js-widget needs additional CSP headers on related page. */
    override def orderPageCsp = Some {
      val subSet = Set.empty[String] + "https://*.yookassa.ru/" + "https://yookassa.ru/" + "https://yoomoney.ru/" + "https://*.yoomoney.ru/" + "https://yastatic.net"
      (
        CspPolicy.defaultSrc.modify( _ ++ subSet ) andThen
        CspPolicy.scriptSrc.modify( _ ++ subSet ) andThen
        CspPolicy.imgSrc.modify(_ ++ subSet ) andThen
        CspPolicy.styleSrc.modify(_ ++ subSet) andThen
        CspPolicy.connectSrc.modify(_ ++ subSet)
      )
    }

    override def payWidgetJsScriptUrl = Some( "https://yookassa.ru/checkout-widget/v1/checkout-widget.js" )
  }


  def default: MPaySystem = YooKassa

  override def values = findValues

}


/** Класс модели платёжной системы. */
sealed abstract class MPaySystem(override val value: String) extends StringEnumEntry {

  /** Информация о поддерживаемой валюте платежной системы.
    *
    * @param currency Интересующая валюта.
    * @return Опциональный инфа по поддерживаемой валюте.
    *         None, если валюта не поддерживается.
    */
  def supportedCurrency(currency: MCurrency): Option[ICurrencyPayInfo]

  /** id узла платежной системы в s.io, если он есть или будет когда-нибудь.
    * Крайне желательно, чтобы был человеко-читабельный id.
    *
    * Не обязательно узел существует, просто его id будет прописан в статистике/транзакциях или иных местах.
    */
  def nodeIdOpt: Option[String]

  /** @return Код локализованного названия ПС по messages. */
  def nameI18n: String

  /** Бывает нужно переопределить хидер X-Frame-Options для HTTP-ответов юзерам, возвращающимся из ПС после оплаты.
    * Тут значение ссылки для случая ALLOW-FROM.
    */
  def returnRespHdr_XFrameOptions_AllowFrom: Option[String] = None

  def orderPageCsp: Option[CspPolicy => CspPolicy] = None

  def payWidgetJsScriptUrl: Option[String]
}

object MPaySystem {

  @inline implicit def univEq: UnivEq[MPaySystem] = UnivEq.derive

  implicit def paySystemJson: Format[MPaySystem] =
    EnumeratumUtil.valueEnumEntryFormat( MPaySystems )

  implicit def paySystemQsB(implicit strB: QsBindable[String]): QsBindable[MPaySystem] =
    EnumeratumUtil.qsBindable( MPaySystems )

}
