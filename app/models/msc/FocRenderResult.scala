package models.msc

import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.sc.focus.FocusedRenderNames._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.06.15 18:32
 * Description: Контейнер данных по результату focused-рендера, появившийся в Focused V2 API.
 * Помимо самого рендера, контейнер хранит метаданные рендера.
 */
object FocRenderResult {

  /** Сериализатор экземпляров модели в JSON.
    * val, потому что вызывается очень часто как в целом, так и в рамках каждого focused-запроса выдачи. */
  implicit val writes: Writes[FocRenderResult] = (
    (__ \ HTML_FN).write[String] and
    (__ \ MODE_FN).write[MFocRenderMode] and
    (__ \ INDEX_FN).write[Int]
  )(unlift(unapply))

}

trait IFocRenderResult {

  /** Отрендеренный и минифицированный html, готовый к заворачиванию в JsString. */
  def html: String

  /** Использованный режим рендера. */
  def mode: MFocRenderMode

  /** Человеческий порядковый номер карточки в выборке.  */
  def index: Int
}


/** Дефолтовая реализация экземпляров модели [[IFocRenderResult]]. */
case class FocRenderResult(
  html  : String,
  mode  : MFocRenderMode,
  index : Int
)
  extends IFocRenderResult
