package io.suggest.sys.mdr

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.10.18 16:36
  * Description: Модель-контейнер qs-данных с описанием команды модерации, отправляемых на сервер.
  */
object MMdrResolution {

  object Fields {
    val NODE_ID_FN      = "n"
    val INFO_FN         = "i"
    val REASON_FN       = "r"
    val MDR_CONF_FN     = "c"
  }

  /** Поддержка play-json, чтобы прогнать через js-router в qs. */
  implicit def mMdrResolutionFormat: OFormat[MMdrResolution] = {
    val F = Fields
    (
      (__ \ F.NODE_ID_FN).format[String] and
      (__ \ F.INFO_FN).format[MMdrActionInfo] and
      (__ \ F.REASON_FN).formatNullable[String] and
      (__ \ F.MDR_CONF_FN).format[MMdrConf]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MMdrResolution] = UnivEq.derive

}


/** Контейнер данных для MdrAction.
  *
  * @param nodeId id модерируемого узла.
  * @param info Данные экшена.
  * @param reason Причина отказа в размещении.
  * @param conf Конфиг формы.
  */
case class MMdrResolution(
                           nodeId       : String,
                           info         : MMdrActionInfo,
                           reason       : Option[String],
                           // TODO Поле отчасти дублируется в info.directSelfId, надо проунифицировать как-то этот вопрос...
                           conf         : MMdrConf,
                         ) {

  def isApprove: Boolean = reason.isEmpty

}
