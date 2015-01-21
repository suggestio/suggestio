package models.adv.js.ctx

import models.adv.JsExtTarget
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.01.15 17:45
 * Description: Модель-враппер над json для быстрого прозрачного доступа к служебным данным.
 * Динамические части моделей существуют как в виде json-обёрток, так и в виде нормальных моделей.
 * Это сделано, т.к. далеко не всегда нужно что-то парсить и менять в контексте, а полный парсинг контекста
 * в будущем может стать ресурсоёмким процессом.
 */
object MJsCtx {

  val ADS_FN      = "_ads"
  val TARGET_FN   = "_target"

  /** mapper из JSON. */
  implicit def reads: Reads[MJsCtx] = (
    (__ \ ADS_FN).readNullable[Seq[MAdCtx]].map(_ getOrElse Nil) and
    (__ \ TARGET_FN).readNullable[JsExtTarget]
  )(apply _)

  /** unmapper в JSON. */
  implicit def writes: Writes[MJsCtx] = (
    (__ \ ADS_FN).write[Seq[MAdCtx]] and
    (__ \ TARGET_FN).writeNullable[JsExtTarget]
  )(unlift(unapply))

}


/**
 * Полноценный контекст, удобный для редактирования через copy().
 * @param mads Данные по рекламным карточкам.
 */
case class MJsCtx(
  mads      : Seq[MAdCtx] = Seq.empty,
  target    : Option[JsExtTarget] = None
)

