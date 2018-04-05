package io.suggest.adn.edit

import io.suggest.model.n2.node.meta.MMetaPub
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 16:36
  * Description: Модель данных узла.
  */
object MAdnEditForm {

  implicit def univEq: UnivEq[MAdnEditForm] = UnivEq.derive

  /** Поддержка play-json. */
  implicit def mAdnEditFormFormat: OFormat[MAdnEditForm] = {
    (__ \ "m").format[MMetaPub]
      .inmap(apply, _.mmeta)
  }

}


/** Контейнер итоговых (и исходных) данных формы редактирования узла-ресивера.
  *
  * @param mmeta Текстовые метаданные узла.
  */
case class MAdnEditForm(
                         mmeta: MMetaPub
                         // TODO Эджи для Welcome, Gallery, Logo
                       )
