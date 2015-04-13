package models.mext.tw.card

import models.{ISize2di, Context}
import play.twirl.api.Html

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.04.15 19:11
 * Description: Контейнер для аргументов рендера twitter card photo.
 */
trait IPhotoCardArgs extends ICardArgs with ImgWhOpt with ImgUrl {
  override type W = IPhotoCardArgs

  override final def cardType = CardTypes.Photo

  /** Рендер шаблона. */
  override def render()(implicit ctx: Context): Html = cardType.template(this)
}

trait IPhotoCardArgsWrapper
  extends CardArgsWrapper
  with ImgUrlWrapper
  with ImgWhOptWrapper
  with IPhotoCardArgs


/** Дефолтовая реализация [[IPhotoCardArgs]]. */
case class PhotoCardArgs(
  imgUrl  : String,
  url     : Option[String],
  title   : Option[String],
  imgWh   : Option[ISize2di]
) extends IPhotoCardArgs
