package models.mext.tw.card

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.04.15 19:12
 * Description: Поддержка поля с обязательной ссылкой на картинку.
 */
trait ImgUrl extends ICardArgsBase {
  override type W <: ImgUrl

  def imgUrl: String
}

/** Враппер для трейта [[ImgUrl]]. */
trait ImgUrlWrapper extends ImgUrl with ICardArgsWrapper {
  override def imgUrl = _cardArgsUnderlying.imgUrl
}
