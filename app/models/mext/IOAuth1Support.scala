package models.mext

import io.suggest.model.geo.GeoPoint
import io.suggest.ym.model.common.MImgInfoMeta
import models.{MAd, MImgSizeT}
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

  /** Сделать пост в сервисе. */
  def mkPost(mad: MAd, acTok: RequestToken, geo: Option[GeoPoint] = None)
            (implicit ws: WSClient, ec: ExecutionContext): Future[IExtPostInfo]
}
