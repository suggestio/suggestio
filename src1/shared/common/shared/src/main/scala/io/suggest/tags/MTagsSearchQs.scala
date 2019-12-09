package io.suggest.tags

import TagSearchConstants.Req._
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.09.15 20:43
 * Description: Модель аргументов для поиска тегов.
 */
object MTagsSearchQs {

  /** Поддержка play-json. */
  implicit def tagSearchArgsJson: OFormat[MTagsSearchQs] = (
    (__ \ FACE_FTS_QUERY_FN).format[String] and
    (__ \ LIMIT_FN).formatNullable[Int] and
    (__ \ OFFSET_FN).formatNullable[Int]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MTagsSearchQs] = UnivEq.derive

}


/** Дефолтовая реализация модели аргументов поискового запроса тегов. */
case class MTagsSearchQs(
                          faceFts      : String,
                          limit        : Option[Int]       = None,
                          offset       : Option[Int]       = None,
                        )
