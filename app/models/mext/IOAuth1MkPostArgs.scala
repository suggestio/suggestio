package models.mext

import io.suggest.model.geo.GeoPoint
import models.adv.{MExtReturn, MExtTarget}
import models.MNode
import play.api.libs.oauth._
import util.ext.IOAuth1Support


/** Аргументы для вызова [[IOAuth1Support]].mkPost(). */
trait IOAuth1MkPostArgs {

  /** Экземпляр рекламной карточки. */
  def mad: MNode

  /** access token. */
  def acTok: RequestToken

  /** Геоинфа. */
  def geo: Option[GeoPoint]

  /** Узел, с которого идёт постинг. */
  def mnode: MNode

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
