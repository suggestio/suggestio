package io.suggest.adn.edit

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 16:09
  * Description: Форма конфигурации формы редактирования узла-ресивера.
  */
object MAdnEditFormConf {

  implicit def univEq: UnivEq[MAdnEditFormConf] = UnivEq.derive

  /** Поддержка play-json. */
  implicit def mAdnEditFormConfFormat: OFormat[MAdnEditFormConf] = {
    (__ \ "i").format[String]
      .inmap[MAdnEditFormConf](apply, _.nodeId)
  }

}


/** Конфиг формы редактирования узла.
  *
  * @param nodeId id редактируемого узла.
  */
case class MAdnEditFormConf(
                             nodeId: String
                           )
