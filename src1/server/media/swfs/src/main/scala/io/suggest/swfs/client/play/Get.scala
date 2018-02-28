package io.suggest.swfs.client.play

import io.suggest.swfs.client.proto.file.FileOpUnknownResponseException
import io.suggest.swfs.client.proto.get.{GetResponse, IGetRequest}
import play.api.http.HeaderNames

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 11:40
 * Description: Чтение файла из хранилища.
 */
trait Get extends ISwfsClientWs {

  override def get(args: IGetRequest): Future[Option[GetResponse]] = {
    val url = args.toUrl

    val startMs = System.currentTimeMillis()
    lazy val logPrefix = s"get($startMs):"
    LOGGER.trace(s"$logPrefix Starting GET $url\n $args")

    var wsReqBuilder = wsClient.url( url )

    // Поддержка сжатых ответов.
    lazy val acceptEncoding = args.acceptCompression.iterator.map(_.httpContentEncoding).mkString(", ")
    if (args.acceptCompression.nonEmpty) {
      wsReqBuilder = wsReqBuilder.addHttpHeaders(
        HeaderNames.ACCEPT_ENCODING -> acceptEncoding
      )
    }

    val streamFut = wsReqBuilder.stream()

    for (ex <- streamFut.failed) {
      LOGGER.error(s"$logPrefix Req failed: GET $url\n $args", ex)
    }

    for {
      sr <- streamFut
      //(headers, enumerator)  <-  streamFut
    } yield {

      val respStatus = sr.status

      if ( SwfsClientWs.isStatus2xx(respStatus) ) {
        val resp = GetResponse( sr.headers, sr.bodyAsSource )
        LOGGER.trace(s"$logPrefix GET OK, status = $respStatus, compressed?${resp.compression}($acceptEncoding), took ${System.currentTimeMillis - startMs}ms")
        Some( resp )
      } else if (respStatus == 404) {
        LOGGER.debug(s"$logPrefix GET $url => 404, took ${System.currentTimeMillis - startMs}ms")
        None
      } else {
        LOGGER.warn(s"Unexpected response for GET $url => $respStatus")
        throw FileOpUnknownResponseException("GET", url, respStatus, None)
      }
    }
  }

}
