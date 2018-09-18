package io.suggest.model.n2.extra.domain

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.{EnumeratumJvmUtil, EnumeratumUtil}
import io.suggest.playx.FormMappingUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.09.16 17:43
  * Description: Модель режимов интеграции между сторонним доменов и s.io.
  *
  * gen 1 : Поддержка только режима отображения выдачи s.io на стороннем домене.
  * gen ~2: Поддерка режима индексации домена с помощью s.io-кравлера.
  */
object MDomainModes extends StringEnum[MDomainMode] {

  /**
    * Режим обслуживания домена, когда владелец в DNS выставляет всё так,
    * чтобы запросы с домена попадали на сервера s.io.
    * Тогда, активация этого режима включает подхватывание входящих http-запросов выдачей sio на текущем узле.
    */
  case object ScServeIncomingRequests extends MDomainMode("sc")

  override val values = findValues

}


sealed abstract class MDomainMode(override val value: String) extends StringEnumEntry {

  /** Код названия по базе play i18n messages. */
  def i18nCode = "Domain.mode." + value

}


object MDomainMode {

  @inline implicit def univEq: UnivEq[MDomainMode] = UnivEq.derive

  def mappingOpt = EnumeratumJvmUtil.stringIdOptMapping( MDomainModes )
  def mapping = FormMappingUtil.optMapping2required( mappingOpt )

  implicit def MDOMAIN_MODE_FORMAT: Format[MDomainMode] = {
    EnumeratumUtil.valueEnumEntryFormat( MDomainModes )
  }

}
