package models.im

import io.suggest.model.{EnumMaybeWithName, EnumValue2Val}
import play.api.data.Mapping
import play.api.mvc.QueryStringBindable
import util.FormUtil
import scala.language.implicitConversions

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.14 17:46
 * Description: Файловые форматы изображений. Изначально были выходными форматами, но это в целом необязательно.
 * 2015.mar.13: Рефакторинг модели, добавление поддержки QueryStringBindable.
 */

object OutImgFmts extends Enumeration with EnumValue2Val with EnumMaybeWithName {

  /**
   * Экземпляр этой модели.
   * @param name Название формата маленькими буквами.
   */
  protected abstract class Val(val name: String) extends super.Val(name) {
    def mime: String
    override def toString() = name
  }

  override type T = Val

  val JPEG: T = new Val("jpeg") {
    override def mime = "image/jpeg"
  }

  val PNG: T = new Val("png") {
    override def mime = "image/png"
  }

  val GIF: T = new Val("gif") {
    override def mime = "image/gif"
  }

  val SVG: T = new Val("svg") {
    override def mime = "image/svg+xml"
  }

  /**
   * Предложить формат для mime-типа.
   * @param mime Строка mime-типа.
   * @return OutImgFmt. Если не-image тип, то будет IllegalArgumentException.
   */
  def forImageMime(mime: String): Option[T] = {
    values
      .find(_.mime equalsIgnoreCase mime)
      .asInstanceOf[Option[T]]
  }


  /** query string биндер для этой модели. */
  implicit def qsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[T] = {
    new QueryStringBindable[T] {
      /** Биндинг значения из карты аргументов. */
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] = {
        strB.bind(key, params).map {
          _.right.flatMap { fmtName =>
            maybeWithName(fmtName) match {
              case Some(fmt) => Right(fmt)
              case None => Left("Unknown image format: " + fmtName)
            }
          }
        }
      }

      /** Сериализация значения. */
      override def unbind(key: String, value: T): String = {
        strB.unbind(key, value.name)
      }
    }
  }


  // Поддержка этой модели в формах.
  import play.api.data.Forms._

  /** Общий код required и optional маппингов здесь. Тут optional-маппинг для хоть как-нибудь заданных значений поля. */
  private def optMappingDirty: Mapping[Option[T]] = {
    text(maxLength = 10)
      .transform [Option[T]] (
        FormUtil.strTrimSanitizeLowerF andThen { maybeWithName },
        _.fold("")(_.name)
      )
  }

  /** Опциональный form-маппер для экземпляров этой модели. */
  def optMapping: Mapping[Option[T]] = {
    optional(optMappingDirty)
      .transform [Option[T]] (_.flatten, Some.apply)
  }

  /** Обязательный form-mapper для экземпляров этой модели. */
  def mapping: Mapping[T] = {
    optMappingDirty
      .verifying("error.required", _.isDefined)
      .transform(_.get, Some.apply)
  }

}

