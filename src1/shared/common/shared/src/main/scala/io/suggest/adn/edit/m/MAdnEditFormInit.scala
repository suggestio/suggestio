package io.suggest.adn.edit.m

import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 16:34
  * Description: Модель данных инициализации формы редактирования узла.
  */

object MAdnEditFormInit {

  implicit def univEq: UnivEq[MAdnEditFormInit] = UnivEq.derive

  /** Поддержка play-json. */
  implicit def mAdnEditFormInitFormat: OFormat[MAdnEditFormInit] = (
    (__ \ "c").format[MAdnEditFormConf] and
    (__ \ "f").format[MAdnEditForm]
  )(apply, unlift(unapply))

}


/** Класс-контейнер данных инициализации формы редактирования ADN-узла.
  *
  * @param conf Конфигурация узла.
  * @param form Контейнер данных формы.
  */
case class MAdnEditFormInit(
                             conf     : MAdnEditFormConf,
                             form     : MAdnEditForm
                           )
