package models.mext

import io.suggest.model.geo.GeoPoint
import io.suggest.ym.model.common.MImgInfoMeta
import models.adv.{MExtTarget, MExtReturn}
import models.{MAdnNode, MAd, MImgSizeT}
import play.api.libs.oauth._
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 19:11
 * Description: Поддержка OAuth1 на сервисе описывается реализацией этого интерфейса.
 */

trait IOAuth1Support {
  /** Доступ к oauth-клиенту для логина и получения access_token'а. */
  def client: OAuth

  /** Быстрый доступ к ключу сервиса. Обычно перезаписывается в реализациях и не зависит от клиента. */
  def consumerKey: ConsumerKey = client.info.key

  /** В каких размерах должно открываться окно авторизации OAuth1. */
  def popupWndSz: MImgSizeT = MImgInfoMeta(height = 400, width = 400)

  /** Проверка валидности access_token'a силами модели. */
  def isAcTokValid(acTok: RequestToken)(implicit ws: WSClient, ec: ExecutionContext): Future[Boolean]

  def sigCalc(acTok: RequestToken) = OAuthCalculator(consumerKey, acTok)

  /**
   * Запостить твит через OAuth1.
   * @param args Данные для постинга.
   * @return Фьючерс с результатом работы.
   */
  def mkPost(args: IOa1MkPostArgs)(implicit ws: WSClient, ec: ExecutionContext): Future[IExtPostInfo]
}


/** Аргументы для вызова [[IOAuth1Support]].mkPost(). */
trait IOa1MkPostArgs {
  /** Экземпляр рекламной карточки. */
  def mad: MAd
  /** access token. */
  def acTok: RequestToken
  /** Геоинфа. */
  def geo: Option[GeoPoint]
  /** Узел, с которого идёт постинг. */
  def mnode: MAdnNode
  /** Генератор ссылки возврата для юзера. */
  def returnTo: MExtReturn
  /** Таргет, т.е. цель размещения. */
  def target: MExtTarget
}
