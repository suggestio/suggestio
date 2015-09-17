package models.mtag

import play.api.libs.json.{Writes, __}
import play.api.libs.functional.syntax._
import io.suggest.sc.TagSearchConstants.Resp._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.09.15 18:34
 * Description: Модель ответа на поисковый запрос тегов. Сериализуется в JSON и отправляется клиенту.
 */
object MTagSearchResp {

  /** Сериализатор в JSON. */
  implicit def WRITES: Writes[MTagSearchResp] = (
    (__ \ RENDERED_FN).writeNullable[String] and
    (__ \ FOUND_COUNT_FN).write[Int]
  )(unlift(unapply))

}


/**
 * Экземпляр модели.
 * @param rendered Отрендеренный HTML куска списка найденных тегов.
 * @param foundCount Кол-во найденных тегов.
 */
case class MTagSearchResp(
  rendered    : Option[String],
  foundCount  : Int
)
