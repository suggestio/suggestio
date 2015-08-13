package models.msc

import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.sc.focus.FocAdProto._

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
    (__ \ MAD_ID_FN).write[String] and
    (__ \ BODY_HTML_FN).write[String] and
    (__ \ CONTROLS_HTML_FN).write[String] and
    (__ \ PRODUCER_ID_FN).write[String] and
    (__ \ INDEX_FN).write[Int]
  )(unlift(unapply))

}


/** Интерфейс класса модели. Скорее всего бесполезен. */
trait IFocRenderResult {

  /** id рекламной карточки. */
  def madId: String

  /** Отрендеренный контент рекламной карточки. */
  def body: String

  /** Отрендеренная верстка заголовка, стрелочек и прочего. */
  def controls: String

  /** id продьсера карточки. */
  def producerId: String

  /** Человеческий порядковый номер карточки в выборке.  */
  def index: Int

}


/** Дефолтовая реализация экземпляров модели [[IFocRenderResult]]. */
case class FocRenderResult(
  madId       : String,
  body        : String,
  controls    : String,
  producerId  : String,
  index       : Int
)
  extends IFocRenderResult
