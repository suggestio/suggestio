package io.suggest.www.m.mreq

import play.api.mvc.{Request, RequestHeader}


/**
 * Враппер над RequestHeader.
 * Аналога на стороне play не существует.
 */
trait IRequestHeaderWrap extends RequestHeader {

  def request: RequestHeader

  override def id             = request.id
  override def secure         = request.secure
  override def uri            = request.uri
  override def remoteAddress  = request.remoteAddress
  override def queryString    = request.queryString
  override def method         = request.method
  override def headers        = request.headers
  override def path           = request.path
  override def version        = request.version
  override def tags           = request.tags
  override def clientCertificateChain = request.clientCertificateChain

}

/** Абстрактная реализация [[IRequestHeaderWrap]]. */
abstract class MRequestHeaderWrapAbstract
  extends IRequestHeaderWrap

/** Дефолтовая реализация [[IRequestHeaderWrap]]. */
case class MRequestHeaderWrap(override val request: RequestHeader)
  extends MRequestHeaderWrapAbstract



/**
 * Враппер над request.
 * Это аналог play RequestWrapper, но трейт, а не класс.
 */
trait IRequestWrap[A] extends Request[A] with IRequestHeaderWrap {
  override def request: Request[A]
  override def body: A = request.body
}

/** Абстрактная реализация [[IRequestWrap]]. */
abstract class MRequestWrapAbstract[A]
  extends MRequestHeaderWrapAbstract
  with IRequestWrap[A]

/** Дефолтовая реализация [[IRequestWrap]]. */
case class MRequestWrap[A](override val request: Request[A])
  extends MRequestWrapAbstract[A]
