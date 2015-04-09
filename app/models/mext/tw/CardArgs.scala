package models.mext.tw

import models.adv.MExtServices

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.04.15 18:14
 * Description: Интерфейсы контейнеров и контейнеры для аргументов рендера различных twitter card.
 * @see [[https://dev.twitter.com/cards/overview]]
 */
trait ICardArgs {

  /** Тип карточки. Заполняется промежуточными реализациями. */
  def cardType: CardType

  /** canonical url страницы. */
  def url: Option[String]

  /** заголовок, отображаемый например в img.alt. */
  def title: Option[String]

  /** Юзернейм приложения на твиттере. По идее он всегда одинаковый. */
  def site: Option[String] = MExtServices.TWITTER.myUserName

}


/** Интерфейс generic-враппера. */
trait ICardArgsWrapper {
  type W

  /** Заврапанные данные. */
  def _cardArgsUnderlying: W
}


/** Враппер для [[ICardArgs]]. */
trait CardArgsWrapper extends ICardArgs with ICardArgsWrapper {
  override type W <: ICardArgs

  override def cardType = _cardArgsUnderlying.cardType
  override def url      = _cardArgsUnderlying.url
  override def title    = _cardArgsUnderlying.title
  override def site     = _cardArgsUnderlying.site
}


trait CardArgsDflt extends ICardArgs {
  override def url    : Option[String] = None
  override def title  : Option[String] = None
}
