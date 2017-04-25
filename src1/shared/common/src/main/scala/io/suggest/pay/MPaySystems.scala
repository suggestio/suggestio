package io.suggest.pay

import enumeratum._
import io.suggest.bill._
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.MsgCodes
import io.suggest.primo.IStrId

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.04.17 11:55
  * Description: Enum-модель платёжных систем.
  */

/** Класс модели платёжной системы. */
sealed abstract class MPaySystem extends EnumEntry with IStrId {

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

}


/** Платёжные системы. */
object MPaySystems extends Enum[MPaySystem] {


  /** Yandex.Kassa. */
  case object YaKa extends MPaySystem {

    override def strId = "yaka"

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

  }


  /** Все платёжные системы. */
  override def values = findValues

}


/** Интерфейс для инстансов, содержащих поле платежной системы. */
trait IMPaySystem {
  def paySystem: MPaySystem
}
