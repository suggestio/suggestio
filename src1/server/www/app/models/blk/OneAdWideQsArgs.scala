package models.blk

import io.suggest.xplay.qsb.QueryStringBindableImpl
import play.api.data.Mapping
import play.api.mvc.QueryStringBindable

import scala.language.implicitConversions   // конверсий тут по факту нет.

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.04.15 16:24
 * Description: Модель для задания wide-параметров (параметров широкого рендера) в qs.
 */

object OneAdWideQsArgs {

  /** Кусок qs-названия поля ширины. */
  def WIDTH_FN = "w"

  /** Поддержка биндинга в routes и в qsb. */
  implicit def oneAdWideQsArgsQsb(implicit intB: QueryStringBindable[Int]): QueryStringBindable[OneAdWideQsArgs] = {
    new QueryStringBindableImpl[OneAdWideQsArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, OneAdWideQsArgs]] = {
        // Оформлено многословно через for{}, т.к. в будущем очень возможно расширения списка аргументов.
        for {
          maybeWidth <- intB.bind( key1(key,WIDTH_FN), params )
        } yield {
          for {
            width <- maybeWidth
          } yield {
            OneAdWideQsArgs(
              width = width
            )
          }
        }
      }

      override def unbind(key: String, value: OneAdWideQsArgs): String = {
        intB.unbind( key1(key, WIDTH_FN), value.width)
      }
    }
  }

  import play.api.data.Forms._

  /** Маппер модели для play form. */
  def mapper: Mapping[OneAdWideQsArgs] = {
    mapping(
      "width" -> number(min = 1, max = 4096)
    )
    { apply }
    { unapply }
  }

  /** Опциональный маппер модели для play form. */
  def optMapper: Mapping[Option[OneAdWideQsArgs]] = {
    optional(mapper)
  }

}


/**
 * В случае ссылки wide-карточку, необходима дополнительная инфа.
 * Высота высчитывается через szMult и исходную высоту.
 * @param width Ширина "экрана" и результата.
 */
case class OneAdWideQsArgs(
  width: Int
)
