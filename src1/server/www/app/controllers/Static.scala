package controllers

import java.nio.charset.StandardCharsets
import javax.inject.{Inject, Singleton}

import akka.stream.scaladsl.{Compression, Flow, Keep, Sink}
import akka.util.ByteString
import controllers.cstatic.{CorsPreflight, RobotsTxt, SiteMapsXml}
import io.suggest.brotli.{BrotliCompress, BrotliUtil}
import io.suggest.compress.{MCompressAlgo, MCompressAlgos}
import io.suggest.ctx.{MCtxId, MCtxIds}
import io.suggest.maps.nodes.{MGeoNodePropsShapes, MGeoNodesResp}
import io.suggest.model.n2.node.MNode
import io.suggest.pick.PickleUtil
import io.suggest.primo.Var
import io.suggest.sec.csp.CspViolationReport
import io.suggest.stat.m.{MComponents, MDiag}
import io.suggest.streams.{ByteStringsChunker, StreamsUtil}
import io.suggest.util.CompressUtilJvm
import models.mctx.Context
import models.mproj.ICommonDi
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.ActorFlow
import play.api.mvc.{BodyParser, WebSocket}
import util.acl._
import util.adv.geo.AdvGeoRcvrsUtil
import util.cdn.CorsUtil
import util.sec.CspUtil
import util.seo.SiteMapUtil
import util.stat.StatUtil
import util.ws.{MWsChannelActorArgs, WsChannelActors}
import util.xplay.SecHeadersFilterUtil
import views.html.static._

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Authors: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  *          Alexander Pestrikov <alexander.pestrikov@cbca.ru>
  * Date: 16.05.13 13:34
  *
  * Description: Изначально это был контроллер для всякой статики.
  * Но постепенно стал контроллером для разных очень общих вещей.
  */
@Singleton
class Static @Inject() (
                         override val ignoreAuth         : IgnoreAuth,
                         override val corsUtil           : CorsUtil,
                         override val siteMapUtil        : SiteMapUtil,
                         isAuth                          : IsAuth,
                         bruteForceProtect               : BruteForceProtect,
                         advGeoRcvrsUtil                 : AdvGeoRcvrsUtil,
                         statUtil                        : StatUtil,
                         streamsUtil                     : StreamsUtil,
                         aclUtil                         : AclUtil,
                         compressUtilJvm                 : CompressUtilJvm,
                         secHeadersFilterUtil            : SecHeadersFilterUtil,
                         cspUtil                         : CspUtil,
                         mCtxIds                         : MCtxIds,
                         isSuOrDevelOr404                : IsSuOrDevelOr404,
                         maybeAuth                       : MaybeAuth,
                         canOpenWsChannel                : CanOpenWsChannel,
                         wsChannelActors                 : WsChannelActors,
                         assets                          : Assets,
                         override val mCommonDi          : ICommonDi
                       )
  extends SioControllerImpl
  with RobotsTxt
  with SiteMapsXml
  with CorsPreflight
{

  import mCommonDi._
  import cspUtil.Implicits._
  import compressUtilJvm.Implicits._
  import streamsUtil.Implicits._


  /**
   * Страница с политикой приватности.
   * @return 200 Ok и много букв.
   */
  def privacyPolicy = maybeAuth() { implicit request =>
    Ok( privacyPolicyTpl() )
      .withHeaders( CACHE_CONTROL -> "public, max-age=600" )
  }

  /** Содержимое проверочного попап-окна. */
  def popupCheckContent = maybeAuth() { implicit request =>
    Ok(popups.popupCheckTpl()).withHeaders(
      CACHE_CONTROL -> "public, max-age=86400"
    )
  }

  /**
   * Костыль в связи с проблемами в play-html-compressor в play-2.3 https://github.com/mohiva/play-html-compressor/issues/20
   * Без этого костыля, запрос html'ки просто подвисает.
   */
  def tinymceColorpicker(filename: String) = maybeAuth() { implicit request =>
    Ok(tinymce.colorpicker.indexTpl())
      .withHeaders(
        CACHE_CONTROL                                 -> "public, max-age=3600",
        secHeadersFilterUtil.X_FRAME_OPTIONS_HEADER   -> "SAMEORIGIN"
      )
  }

  /**
   * Доступ к привилегированным ассетам: js.map и прочие сорцы.
   * @param path Путь.
   * @param asset filename.
   * @return Экшен раздачи ассетов с сильно урезанным кешированием на клиенте.
   */
  def vassetsSudo(path: String, asset: Assets.Asset) = isSuOrDevelOr404().async { implicit request =>
    // TODO Запретить раздачу привелигированных ассетов через CDN в продакшене? Чтобы отладка главной страницы шла только по vpn.
    val resFut = assets.versioned(path, asset)(request)
    // Для привелегированных ассетов нужно запретить промежуточные кеширования.
    resFut.map { res =>
      val ttl = if (isProd) 300 else 10
      res.withHeaders(CACHE_CONTROL -> s"private, max-age=$ttl")
    }
  }

  def assetsSudo(path: String, asset: Assets.Asset) = vassetsSudo(path, asset)


  /**
   * Экшен для скрытого продления сессии в фоне. Может дергаться в js'ом незаметно.
   * @return 204 No Content - всё ок.
   *         Другой код - сессия истекла.
   */
  def keepAliveSession = isAuth() { implicit request =>
    NoContent
  }


  /** Body-parser для CSP-репортов.
    * CSP-report -- это JSON в особом формате. Проверить Content-Type, затем распарсить просто как JSON.
    * Валидировать JSON будет уже экшен.
    */
  private def _cspJsonBp: BodyParser[JsValue] = parse.when(
    predicate = _.contentType.exists { m =>
      m.equalsIgnoreCase( "application/csp-report" ) ||
        m.equalsIgnoreCase( JSON )
    },
    // тестовый реальный отчёт из firefox весил 631 байт.
    parser = parse.tolerantJson( 2048 ),
    badResult = { req0 =>
      val req = aclUtil.reqHdrFromRequestHdr(req0)
      LOGGER.debug(s"_cspBp: Dropped request from ${req.remoteClientAddress} with unknown content type ${req.contentType}")
      // Нет никакой надобности отвечать html-страницами, т.к. на CSP-экшены никто кроме браузеров не обращается.
      UnsupportedMediaType( "CSP-report expected" )
    }
  )



  /** Экшен принятия отчёта об ошибке CSP от браузера.
    * Какой-то браузер жалуется на нарушение политики безопасности контента (CSP) на сайте.
    * Экшен не очень уместен для этого контроллера (Static), но в целом ок.
    *
    * Этот запрос имеет вид JSON-документа:
    * {{{
    *   {
    *     "csp-report": {
    *       "document-uri": "http://example.com/signup.html",
    *       "referrer": "",
    *       "blocked-uri": "http://example.com/css/style.css",
    *       "violated-directive": "style-src cdn.example.com",
    *       "original-policy": "default-src 'none'; style-src cdn.example.com; report-uri /_/csp-reports"
    *     }
    *   }
    * }}}
    *
    *
    * Реальный отчёт от firefox'а (звёздочки обособлены пробелами, что не ломать коммент в коде):
    * {{{
    *   {
    *     "csp-report": {
    *       "blocked-uri": "data",
    *       "document-uri": "http://cbda.ru/",
    *       "original-policy": "default-src http://suggest.cdnvideo.ru http://cbda.ru blob:; script-src http://suggest.cdnvideo.ru 'unsafe-eval' 'unsafe-inline' blob: http://cbda.ru; style-src http://suggest.cdnvideo.ru http://cbda.ru 'unsafe-inline'; img-src https:// * .mapbox.com http://suggest.cdnvideo.ru data: blob: http://cbda.ru; connect-src http://suggest.cdnvideo.ru http://cbda.ru wss://suggest.io https:// * .mapbox.com; report-uri http://cbda.ru/_/csp/report",
    *       "referrer": "",
    *       "violated-directive": "style-src http://suggest.cdnvideo.ru http://cbda.ru 'unsafe-inline'"
    *     }
    *   }
    * }}}
    * Данная ошибка возникла из-за stylish, который подмешивает стили через data:
    *
    */
  def handleCspReport = bruteForceProtect {
    maybeAuth().async( _cspJsonBp ) { implicit request =>
      // Залить ошибку в MStat.
      lazy val logPrefix = s"cspReportHandler[${System.currentTimeMillis()}]:"
      val requestBodyStr = request.body.toString()
      LOGGER.info(s"$logPrefix From ip#${request.remoteClientAddress} Body:\n $requestBodyStr")

      request.body.validate[CspViolationReport].fold(
        // Ошибка парсинга JSON-тела. Вообще, это обычно неправильно.
        {violations =>
          LOGGER.warn(s"$logPrefix Invalid JSON: ${violations.mkString(", ")}")
          BadRequest("WOW!")
        },

        // Всё ок распарсилось.
        {cspViol =>
          // Сам результат парсинга не особо важен, это скорее контроль контента.
          val userSaOptFut = statUtil.userSaOptFutFromRequest()
          val _ctx = implicitly[Context]

          for {
            _userSaOpt <- userSaOptFut

            stat2 = new statUtil.Stat2 {
              override def ctx = _ctx
              override def uri = Some( cspViol.documentUri )
              override def components = {
                MComponents.CSP :: MComponents.Error :: super.components
              }
              override def statActions = Nil
              override def userSaOpt = _userSaOpt

              override def diag = MDiag(
                message = Some( requestBodyStr )
              )
            }

            r <- statUtil.saveStat(stat2)

          } yield {
            LOGGER.trace(s"$logPrefix Saved csp-report -> $r")
            NoContent
          }
        }
      )
    }
  }


  /** Контейнер для данных ответа сервера на тему карты ресиверов.
    * Абстрагирует всякие особенности json/binary сериализаций от итогов работы.
    *
    * @param contentType Тип содержимого ответа.
    * @param gzip gzip-пожатый ответ.
    * @param etag Фьючерс со значением etag в виде целого.
    * @param brotli Фьючрес с brotli-пожатый ответ.
    */
  private case class MAdvRcvrsMapRespData(
                                           contentType   : String,
                                           gzip          : Future[Var[ByteString]],
                                           etag          : Future[String],
                                           brotli        : Option[Future[Var[ByteString]]] = None
                                         )

  /** Рендерер JSON-карты данных узлов. */
  private def _advRcvrsMapRespJsonFut(): Future[MAdvRcvrsMapRespData] = {
    cache.getOrElseUpdate("advGeoNodesJson", expiration = 10.seconds) {
      val byteStringSink = streamsUtil.byteStringAccSink
        // Запускать в фоне compact, который подменит исходное неоптимизированное значение ByteString.
        .asyncCompactedByteString

      // Продолжаем реактивную обработку, но ответ клиенту будет нереактивным (кэширование, ибо).
      val ((hashCodeFut, gzipFut), brotliFut) = advGeoRcvrsUtil
        .rcvrNodesMap( advGeoRcvrsUtil.onMapRcvrsSearch(30) )
        // Ветвь рассчёта контрольной суммы: материализуем Map[NodeId, data.hashCode()].hashCode()
        .alsoToMat {
          Flow[(MNode, MGeoNodePropsShapes)]
            .map { case (mnode, mpropsShapes) =>
              mnode.id.get -> mpropsShapes.hashCode()
            }
            .toMat {
              Sink.collection[(String, Int), Map[String, Int]]
                .mapMaterializedValue { nodeHashesMapFut =>
                  for (nodeHashesMap <- nodeHashesMapFut) yield {
                    nodeHashesMap
                      .hashCode()
                      .toString + "j"
                  }
                }
            }(Keep.right)
        }(Keep.right)
        // JSON-рендер каждого item'а:
        .map { case (_, data) =>
          Json.toJson(data)
        }
        .jsonSrcToJsonArrayNullEnded
        .map { jsonStrPart =>
          // Для JSON допустим только UTF-8. Явно фиксируем это:
          ByteString.fromString( jsonStrPart, StandardCharsets.UTF_8 )
        }
        // Нормализовать размеры ByteString'ов для входа компрессоров. Для Gzip это влияет на коэфф.сжатия.
        .via( new ByteStringsChunker(8192) )
        // Сжатие в GZIP:
        .alsoToMat {
          Flow[ByteString]
            .via( Compression.gzip )
            .toMat( byteStringSink )(Keep.right)
        }(Keep.both)
        // Сжатие в brotli:
        .toMat {
          Flow[ByteString]
            .via( BrotliUtil.compress )
            .toMat( byteStringSink )(Keep.right)
        }(Keep.both)
        .run()

      val respData = MAdvRcvrsMapRespData(
        contentType = JSON,
        gzip        = gzipFut,
        etag        = hashCodeFut,
        brotli      = Some( brotliFut )
      )

      Future.successful( respData )
    }
  }


  /** Обсчитать данные карты узлов с использование кэширования.
    *
    * @return Фьючерс с gzip-ответом и значением etag.
    */
  private def _advRcvrsMapRespBoopickleFut(): Future[MAdvRcvrsMapRespData] = {
    // Собрать и оформить данные по всем узлам, с использованием кэша.
    // Операция ресурсоёмкая, поэтому кэш обязателен.
    // TODO После портирования назад на JSON, надо будет akka-stream параллельно развести на gzip и brotli компрессоры и на хэшировалку. И опять же, кэшируя результат.
    cache.getOrElseUpdate("advGeoNodesBoo", expiration = 10.seconds) {
      // boo: (gzip, hash):
      for {
        nodesRespMap <- advGeoRcvrsUtil
          .rcvrNodesMap(
            advGeoRcvrsUtil.onMapRcvrsSearch(30)
          )
          .map { case (mnode, data) =>
            mnode.id.get -> data
          }
          // Собрать в единый список всё это дело:
          .toMat {
            Sink.collection[(String, MGeoNodePropsShapes), Map[String, MGeoNodePropsShapes]]
          }( Keep.right )
          .run()

        etagFut = Future {
          nodesRespMap
            .hashCode()
            .toString + "b"
        }

        gzipBytesFut = Future {
          val nodesResp = MGeoNodesResp(
            nodes = nodesRespMap.values
          )
          val bbuf = PickleUtil.pickle( nodesResp )
          val gzippedBytea = GzipCompressConv.convert( bbuf )
          Var( ByteString.fromArrayUnsafe( gzippedBytea ) )
        }

      } yield {
        // Завернуть в ByteString.
        MAdvRcvrsMapRespData(
          contentType = BINARY,
          gzip = gzipBytesFut,
          etag = etagFut
        )
      }
    }
  }


  /**
    * Получение списка шейпов и маркеров узлов-ресиверов на карте.
    *
    * Это ресурсоёмкая операция, использует реактивный доступ к множеству узлов,
    * но при этом возвращает НЕпоточную структуру данных.
    *
    * 2017-06-06: Экшен теперь НЕ проверяет CSRF для возможности кеширования в CDN.
    * В routes вставлена соотв. волшебная комбинация "/~" для защиты от CSRF-настойчивого js-роутера.
    *
    * @return Бинарь с маркерами всех упомянутых узлов + список шейпов.
    */
  def advRcvrsMap = {
    ignoreAuth().async { implicit request =>
      // Завернуть данные в единый блоб и отправить клиенту.
      val accept = request.headers.get( ACCEPT )
      lazy val logPrefix = s"advRcvrsMap(${accept.orNull})#${System.currentTimeMillis()}:"

      val acceptJson = accept.contains( JSON )
      // TODO 2018-03-12 boopickle и bin-формат теперь только для совместимости с кривыми установленными webapp sc3. Можно её удалить ближе к запуску.
      val acceptBinary = accept.contains( BINARY )

      if (acceptJson ^ acceptBinary) {
        for {
          // TODO Проверить Accept, чтобы разрулить варианты между boopickle и json.
          respData <- if (acceptJson)
            _advRcvrsMapRespJsonFut()
          else if (acceptBinary)
            _advRcvrsMapRespBoopickleFut()
          else
            throw new IllegalArgumentException("Accept header invalid")

          etag <- respData.etag

          // Совпадает ли Etag со значением на клиенте?
          etagMatches = request.headers
            .get(IF_NONE_MATCH)
            .contains( etag )

          resultBase <- {
            if (etagMatches) {
              LOGGER.trace(s"$logPrefix Etag match $etag, returning 304")
              Future.successful( NotModified )

            } else {
              // Пытаемся также вернуть brotli-ответ, т.к. это быстро.
              val (bodyCompressAlgo, bodyCompressedFut) = respData
                .brotli
                .filter { _ =>
                  request.headers
                    .get(ACCEPT_ENCODING)
                    .exists(_ contains MCompressAlgos.Brotli.httpContentEncoding)
                }
                // Наврядли есть клиенты без поддержки gzip, принебрегаем ими:
                .fold [(MCompressAlgo, Future[Var[ByteString]])] (MCompressAlgos.Gzip -> respData.gzip) { MCompressAlgos.Brotli -> _ }

              for {
                respBodyVar <- bodyCompressedFut
              } yield {
                LOGGER.trace(s"$logPrefix Done, $bodyCompressAlgo with ${respBodyVar.value.length} bytes")
                Ok( respBodyVar.value )
                  .as( respData.contentType )
                  .withHeaders(
                    CONTENT_ENCODING -> bodyCompressAlgo.httpContentEncoding
                  )
              }
            }
          }

        } yield {
          resultBase
            .withHeaders(
              // TODO Для 304-ответа тоже надо Etag, Cache-control и Vary? Или только для 200 ok?
              CACHE_CONTROL -> "public, max-age=20",
              ETAG          -> etag,
              VARY          -> (ACCEPT :: ACCEPT_ENCODING :: Nil).mkString(", ")
            )
        }

      } else {
        // Accept: содержит некорректное значение.
        BadRequest( ACCEPT )
      }
    }
  }


  /** Общий websocket-канал обмена данными между клиентом и конкретной нодой.
    * Появилась для объединения всех будущих ws-экшенов, делающих примерно одно и тоже.
    *
    * Здесь может быть недоступна сессия из cookie.
    *
    * @param ctxId Контекстный id текущей страницы, привязанный к текущему юзеру.
    * @return WebSocket, если всё ок.
    */
  def wsChannel(ctxId: MCtxId) = WebSocket.acceptOrResult[JsValue, JsValue] { implicit rh =>
    def logPrefix = s"wsChannel():"

    // Используем match вместо .fold, чтобы не прописывать тут сложный тип вида Future[Either[...,Flow[...]]]
    val res = canOpenWsChannel.can(ctxId) match {
      // Разрешаем коннекшен
      case Some(mrh) =>
        LOGGER.trace(s"$logPrefix Opening connection for user#${mrh.user.personIdOpt.orNull} ctx#${ctxId.key}...")
        val aFlow = ActorFlow.actorRef(
          props = { out =>
            val args = MWsChannelActorArgs(out, ctxId)
            wsChannelActors.props(args)
          }
        )
        Right(aFlow)

      // Не разрешён коннект: чтоо-то нет так с ctxId.
      case None =>
        val ctxIdStr = ctxId.toString
        LOGGER.warn(s"$logPrefix not allowed: $ctxIdStr")
        val result = Forbidden(s"Forbidden: $ctxIdStr")
        Left(result)
    }

    Future.successful(res)
  }

}
