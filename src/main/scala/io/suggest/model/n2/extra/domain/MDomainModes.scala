package io.suggest.model.n2.extra.domain

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.model.menum.EnumJsonReadsValT
import io.suggest.primo.IStrId

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

  override type T = Val

  /**
    * Режим обслуживания домена, когда владелец в DNS выставляет всё так,
    * чтобы запросы с домена попадали на сервера s.io.
    * Тогда, активация этого режима включает подхватывание входящих http-запросов выдачей sio на текущем узле.
    */
  val ScServeIncomingRequests: T = new Val("sc")

}
