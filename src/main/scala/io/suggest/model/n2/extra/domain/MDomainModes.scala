package io.suggest.model.n2.extra.domain

import io.suggest.common.empty.EmptyUtil
import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.model.menum.EnumJsonReadsValT
import io.suggest.primo.IStrId
import play.api.data.Mapping

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.09.16 17:43
  * Description: Модель режимов интеграции между сторонним доменов и s.io.
  *
  * gen 1 : Поддержка только режима отображения выдачи s.io на стороннем домене.
  * gen ~2: Поддерка режима индексации домена с помощью s.io-кравлера.
  */
object MDomainModes extends EnumMaybeWithName with EnumJsonReadsValT {

  /** Класс для всех экземпляров модели. */
  protected class Val(override val strId: String)
    extends super.Val(strId)
    with IStrId
  {

    /** Код названия по базе play i18n messages. */
    def i18nCode = "Domain.mode." + strId

  }

  override type T = Val

  /**
    * Режим обслуживания домена, когда владелец в DNS выставляет всё так,
    * чтобы запросы с домена попадали на сервера s.io.
    * Тогда, активация этого режима включает подхватывание входящих http-запросов выдачей sio на текущем узле.
    */
  val ScServeIncomingRequests: T = new Val("sc")


  /** play form маппинг с опциональным значением режима домена. */
  def mappingOpt: Mapping[Option[MDomainMode]] = {
    import play.api.data.Forms._
    nonEmptyText(minLength = 1, maxLength = 10)
      .transform [Option[T]] (
        { raw => maybeWithName( raw.trim.toLowerCase() ) },
        _.fold("")(_.strId)
      )
  }

  /** play form маппинг с обязательным значением режима домена. */
  def mapping: Mapping[MDomainMode] = {
    mappingOpt
      .verifying("error.required", _.nonEmpty)
      .transform [MDomainMode] ( EmptyUtil.getF[MDomainMode], EmptyUtil.someF[MDomainMode] )
  }

}
