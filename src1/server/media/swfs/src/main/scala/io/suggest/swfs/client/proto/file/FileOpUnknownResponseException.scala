package io.suggest.swfs.client.proto.file

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 11:58
 * Description: Экзепшен, описывающий неожиданный ответ swfs-сервера.
 */
case class FileOpUnknownResponseException(method: String, url: String, httpStatus: Int, respBody: Option[String] = None)
  extends RuntimeException(s"Swfs client failure: $method $url => $httpStatus ||| $respBody")
