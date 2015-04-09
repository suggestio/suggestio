package models.mext.tw

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.04.15 19:11
 * Description: Контейнер для аргументов рендера twitter card photo.
 */
trait IPhotoCardArgs extends ICardArgs with ImgWhOpt with ImgUrl {
  override final def cardType = CardTypes.Photo
}

trait IPhotoCardArgsWrapper extends CardArgsWrapper with ImgUrlWrapper with ImgWhOptWrapper with IPhotoCardArgs {
  override type W <: IPhotoCardArgs
}
