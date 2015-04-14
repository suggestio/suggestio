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

  /** Калькулятор oauth1-сигнатур запросов. */
  def sigCalc(acTok: RequestToken) = OAuthCalculator(consumerKey, acTok)

  /** Необходимо ли делать mp-upload карточки на сервер перед вызовом mkPost?
    * Если true, то текущий сервис должен поддерживать mpUpload. */
  def isMkPostNeedMpUpload: Boolean

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
  /** Приложения к посту, если есть. */
  def attachments: TraversableOnce[IPostAttachmentId]

  override def toString: String = {
    val sb = new StringBuilder(192)
    sb.append( getClass.getSimpleName )
      .append('(')
      .append("mad=").append(mad.id.orNull).append(',')
      .append("acTok=").append(acTok.token).append(',')
    val _geo = geo
    if (_geo.isDefined)
      sb.append("geo=").append(_geo.get).append(',')
    sb.append("node=").append(mnode.id.orNull).append(',')
      .append("returnTo=").append(returnTo.strId).append(',')
      .append("target=").append(target.id.orNull)
    val _atts = attachments
    if (_atts.nonEmpty)
      sb.append(",attachments=[").append(_atts.mkString(",")).append(']')
    sb.append(')')
    sb.toString()
  }
}
