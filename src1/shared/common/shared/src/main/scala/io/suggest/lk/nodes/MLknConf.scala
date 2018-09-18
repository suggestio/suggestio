package io.suggest.lk.nodes

import boopickle.Default._
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.03.17 16:51
  * Description: Кросс-платформенная модель разных важных данных формы.
  * Обычно, она собирается на сервере и неизменно живёт внутри состояния формы.
  */

object MLknConf {

  /** Поддержка бинарной сериализации. */
  implicit val mLknConfPickler: Pickler[MLknConf] = {
    generatePickler[MLknConf]
  }

  @inline implicit def univEq: UnivEq[MLknConf] = UnivEq.derive

  implicit def mLknConfFormat: OFormat[MLknConf] = (
    (__ \ "n").format[String] and
    (__ \ "a").formatNullable[String]
  )(apply, unlift(unapply))

}


case class MLknConf(
                     onNodeId : String,
                     adIdOpt  : Option[String]
                   )
