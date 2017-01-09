package models.req

import play.api.mvc.{Request, RequestHeader}


/**
 * Враппер над RequestHeader.
 * Аналога на стороне play не существует.
 */
trait RequestHeaderWrap extends RequestHeader {

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


/**
 * Враппер над request.
 * Это аналог play RequestWrapper, но трейт, а не класс.
 */
trait RequestWrap[A] extends Request[A] with RequestHeaderWrap {
  override def request: Request[A]
  override def body: A = request.body
}
