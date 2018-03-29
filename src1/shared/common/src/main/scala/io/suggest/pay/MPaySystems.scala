package io.suggest.pay

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.bill._
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.MsgCodes

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
      OptionUtil.maybe( currency == rub ) {
        MCurrencyPayInfo(
          currency              = rub,
          // Нижний лимит: по картам нельзя платить ниже 1 рубля. Поэтому считаем, что платить просто нельзя.
          lowerDebtLimitOpt     = Some( 1d ),
          // Верхний предел: взят с потолка.
          upperDebtLimitOpt     = Some( 1000000d )
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

  }


  /** Все платёжные системы. */
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

}


/** Интерфейс для инстансов, содержащих поле платежной системы. */
trait IMPaySystem {
  def paySystem: MPaySystem
}
