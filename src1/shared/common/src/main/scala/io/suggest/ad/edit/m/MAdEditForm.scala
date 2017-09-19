package io.suggest.ad.edit.m

import io.suggest.jd.MJdEditEdge
import io.suggest.jd.tags.IDocTag
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 17:40
  * Description: Модель состояние формы редактирования рекламной карточки.
  * Основной протокол общения клиента и сервера в вопросе редактирования рекламных карточек.
  */
object MAdEditForm {

  object Fields {
    val TEMPATE_FN = "t"
    val EDGES_FN   = "e"
  }

  /** Поддержка play-json. */
  implicit val MAD_EDIT_FORM_FORMAT: OFormat[MAdEditForm] = (
    (__ \ Fields.TEMPATE_FN).format[IDocTag] and
    // Массив эджей без Nullable, т.к. это очень маловероятная ситуация слишком пустой карточки.
    (__ \ Fields.EDGES_FN).format[Iterable[MJdEditEdge]]
  )(apply, unlift(unapply))

}


/** Класс контейнера данных формы редактирования карточки.
  * Именно этот класс является основным языком связи js-клиента и jvm-сервера.
  *
  * @param template Шаблон документа.
  * @param edges Эджи с данными для рендера документа.
  */
case class MAdEditForm(
                        template    : IDocTag,
                        edges       : Iterable[MJdEditEdge]
                      )
