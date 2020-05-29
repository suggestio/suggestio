package io.suggest.swfs.client.play

import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.suggest.common.empty.OptionUtil
import io.suggest.proto.http.HttpConst
import io.suggest.swfs.client.proto.file.FileOpUnknownResponseException
import io.suggest.swfs.client.proto.get.{GetRequest, GetResponse}
import play.api.http.HeaderNames
import japgolly.univeq._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 11:40
 * Description: Чтение файла из хранилища.
 */
trait Get extends ISwfsClientWs {

  override def get(args: GetRequest): Future[Option[GetResponse]] = {
    val url = args.toUrl

    val startMs = System.currentTimeMillis()
    lazy val logPrefix = s"get($startMs):"
    LOGGER.trace(s"$logPrefix Starting GET $url\n $args")

    var wsReqBuilder = wsClient.url( url )

    // Поддержка сжатых ответов.
    lazy val acceptEncoding = args.params.acceptCompression
      .iterator
      .map(_.httpContentEncoding)
      .mkString(", ")

    var reqHeadersAcc = List.empty[(String, String)]

    if (args.params.acceptCompression.nonEmpty)
      reqHeadersAcc ::= HeaderNames.ACCEPT_ENCODING -> acceptEncoding

    // Отработать range.
    for (dsRange <- args.params.range) {
      reqHeadersAcc ::= HeaderNames.RANGE -> dsRange.range
      for (rangeIf <- dsRange.rangeIf)
        reqHeadersAcc ::= HeaderNames.IF_RANGE -> rangeIf
    }

    if (reqHeadersAcc.nonEmpty)
      wsReqBuilder = wsReqBuilder.addHttpHeaders( reqHeadersAcc: _* )

    val respFut =
      if (args.params.returnBody) wsReqBuilder.stream()
      else wsReqBuilder.head()

    for (ex <- respFut.failed)
      LOGGER.error(s"$logPrefix Req failed: GET $url\n $args", ex)

    for {
      resp <- respFut
    } yield {
      val respStatus = resp.status

      if ( SwfsClientWs.isStatus2xx(respStatus) ) {
        val respBodySrc: Source[ByteString, _] =
          if (args.params.returnBody) resp.bodyAsSource
          else Source.empty
        val isPartial = respStatus ==* HttpConst.Status.PARTIAL_CONTENT

        val getResp = GetResponse(
          headers   = resp.headers,
          data      = respBodySrc,
          isPartial = isPartial,
          httpContentRange = OptionUtil.maybeOpt( isPartial ) {
            resp.header( HttpConst.Headers.CONTENT_RANGE )
          },
        )

        LOGGER.trace(s"$logPrefix GET OK, status = $respStatus, compressed?${getResp.compression}($acceptEncoding), took ${System.currentTimeMillis - startMs}ms")
        Some( getResp )

      } else if (respStatus ==* HttpConst.Status.NOT_FOUND) {
        LOGGER.debug(s"$logPrefix GET $url => 404, took ${System.currentTimeMillis - startMs}ms")
        None

      } else {
        LOGGER.warn(s"Unexpected response for GET $url => $respStatus")
        throw FileOpUnknownResponseException("GET", url, respStatus, None)
      }
    }
  }

}
