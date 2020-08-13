package io.suggest.lk.nodes

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 15:36
  * Description: Кросс-платформенная модель инициализации формы управления узлами.
  */
object MLknFormInit {

  @inline implicit def univEq: UnivEq[MLknFormInit] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

  implicit def mLknFormInitFormat: OFormat[MLknFormInit] = (
    (__ \ "c").format[MLknConf] and
    (__ \ "n").format[MLknNodeResp]
  )(apply, unlift(unapply))

}


/** Класс модели с данными для начальной конфигурацией формы.
  *
  * @param resp0 Начальный список под-узлов, чтобы его не дёргать с сервера.
  */
case class MLknFormInit(
                         conf       : MLknConf,
                         resp0      : MLknNodeResp,
                       )
