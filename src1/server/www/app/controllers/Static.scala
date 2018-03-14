package controllers

import java.nio.charset.StandardCharsets
import javax.inject.{Inject, Singleton}

import akka.NotUsed
import akka.stream.{FlowShape, Graph}
import akka.stream.scaladsl.{Compression, Flow, Keep, Sink, Source}
import akka.util.ByteString
import controllers.cstatic.{CorsPreflight, RobotsTxt, SiteMapsXml}
import io.suggest.brotli.BrotliUtil
import io.suggest.compress.{MCompressAlgo, MCompressAlgos}
import io.suggest.ctx.{MCtxId, MCtxIds}
import io.suggest.maps.nodes.{MGeoNodePropsShapes, MGeoNodesResp}
import io.suggest.model.n2.node.MNodes
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
import play.api.mvc.{BodyParser, Result, WebSocket}
import util.acl._
import util.adv.geo.AdvGeoRcvrsUtil
import util.cdn.CorsUtil
import util.sec.CspUtil
import util.seo.SiteMapUtil
import util.stat.StatUtil
import util.ws.{MWsChannelActorArgs, WsChannelActors}
import util.xplay.SecHeadersFilterUtil
import views.html.static._
import japgolly.univeq._
import play.api.http.{HttpEntity, HttpProtocol}

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
                         mNodes                          : MNodes,
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
                                           // TODO Удалить etag-поле следом за boopickle-сериализацией для advRcvrsMap()
                                           etag          : Future[String],
                                           brotli        : Option[Future[Var[ByteString]]] = None
                                         ) {
    def forCompressAlgo(algo: MCompressAlgo): Future[Var[ByteString]] = {
      algo match {
        case MCompressAlgos.Brotli => brotli.get
        case MCompressAlgos.Gzip   => gzip
      }
    }
  }

  /** Контейнер непоточных данных для ответа клиенту. */
  private case class MAdvRcvrsStrictRespData(
                                              compressAlgo: MCompressAlgo,
                                              body: Future[Var[ByteString]]
                                            )

  /** Рендерер JSON-карты данных узлов.
    *
    * @param respCompressAlgoOpt Какое сжатие требуется клиенту на выходе?
    * @return Фьючерс с результатами.
    *         Left(source) - Source для chunked/streamed выдачи данных юзеру
    *         Right(...) - готовые к употреблению данные.
    */
  private def _advRcvrsMapRespJsonFut(respCompressAlgoOpt: Option[MCompressAlgo]): Future[Either[Source[ByteString, MAdvRcvrsMapRespData], MAdvRcvrsStrictRespData]] = {
    for {
      promiseOrCached <- cacheApiUtil.promiseOrCached[MAdvRcvrsMapRespData]( "advGeoNodesJson", expiration = 10.seconds )
    } yield {
      promiseOrCached.fold(
        // В кэше нет данных, то надо запустить рассчёты:
        {cachedPromise =>
          // Организуем реактивную обработку нод с генерацией несжатых byte chunks:
          val NODES_PER_CHUNK = 30
          val chunkedUncompressedSrc = advGeoRcvrsUtil
            .rcvrNodesMap( advGeoRcvrsUtil.onMapRcvrsSearch(NODES_PER_CHUNK) )
            // JSON-рендер каждого item'а:
            .map { case (_, data) =>
              Json.toJson(data)
            }
            .jsValuesToJsonArray
            .map { jsonStrPart =>
              // Для JSON допустим только UTF-8. Явно фиксируем это:
              ByteString.fromString( jsonStrPart, StandardCharsets.UTF_8 )
            }
            // Нормализовать размеры ByteString'ов для входа компрессоров. Для Gzip это влияет на коэфф.сжатия.
            .via( new ByteStringsChunker(8192) )

          val byteStringSink = streamsUtil.byteStringAccSink
            // Запускать в фоне compact, который подменит исходное неоптимизированное значение ByteString.
            .asyncCompactedByteString

          // Дедубликация кода сборки одного sink'а любого сжатия
          def __compressSink( compressFlow: Graph[FlowShape[ByteString, ByteString], NotUsed]): Sink[ByteString, Future[Var[ByteString]]] = {
            Flow[ByteString]
              .via( compressFlow )
              .toMat( byteStringSink )(Keep.right)
          }

          // TODO Удалить этот костыль следом за .etag-полем и boopickle-сериализацией.
          val etagStub = Future.successful("")
          // Общий код вызова финального mapMaterialzedValue(), который сохранит всё в кэш и вернёт нормализованное значение.
          def __mapMaterialized(gzip    : Future[Var[ByteString]],
                                brotli  : Future[Var[ByteString]]): MAdvRcvrsMapRespData = {
            val r = MAdvRcvrsMapRespData(JSON, gzip, etagStub, Some(brotli))
            cachedPromise.success( r )
            r
          }

          // TODO Opt В будущем можно генерить и кэшировать только brotli-выхлоп, а gzip и uncompressed при необходимости, путём распаковки из brotli на лету.
          val gzipCompressor = Compression.gzip
          val brotliCompressor = BrotliUtil.compress

          // Сборка финального Source на основе желаемого алгоритма сжатия на выходе:
          val respSrc = respCompressAlgoOpt.fold[Source[ByteString, MAdvRcvrsMapRespData]] {
            // Без сжатия, значит собрать два синка
            chunkedUncompressedSrc
              .alsoToMat( __compressSink(gzipCompressor) )(Keep.right)
              .alsoToMat( __compressSink(brotliCompressor) )(Keep.both)
              .mapMaterializedValue[MAdvRcvrsMapRespData] { case (gzipFut, brotliFut) =>
                __mapMaterialized(gzipFut, brotliFut)
              }
          } {
            // Клиент хочет brotli-сжатия. Пересобираем:
            case MCompressAlgos.Brotli =>
              chunkedUncompressedSrc
                .alsoToMat( __compressSink(gzipCompressor) )(Keep.right)
                .via( brotliCompressor )
                .alsoToMat( byteStringSink )(Keep.both)
                .mapMaterializedValue { case (gzipFut, brotliFut) =>
                  __mapMaterialized(gzipFut, brotliFut)
                }
            case MCompressAlgos.Gzip =>
              chunkedUncompressedSrc
                .alsoToMat( __compressSink( brotliCompressor ) )(Keep.right)
                .via(gzipCompressor)
                .alsoToMat( byteStringSink )(Keep.both)
                .mapMaterializedValue { case (brotliFut, gzipFut) =>
                  __mapMaterialized(gzipFut, brotliFut)
                }
          }
          Left(respSrc)
        },
        // Прямое попадание в кэш:
        {allData =>
          // Пока просто форсируем gzip, даже если Accept-Encoding не задан. Наврядли это на кого-либо повлияет.
          // Потом когда-нибудь можно впилить тут gunzip для честной выдачи без сжатия.
          val compAlgo = respCompressAlgoOpt.getOrElse( MCompressAlgos.Gzip )
          val respData = MAdvRcvrsStrictRespData(
            compressAlgo = compAlgo,
            body = allData.forCompressAlgo( compAlgo )
          )
          Right(respData)
        }
      )
    }
  }

  /** Экшен потоковой генерации карты ресиверов.
    * Объединяет в себе потоковый ответ наравне с кэшированием всех собранных значений.
    *
    * @return Ok + Streamed/Chunked (заполнения кэша)
    *         Ok + Strict (извлечение из кэша)
    */
  def advRcvrsMapJson = {
    ignoreAuth().async { implicit request =>
      // Быстро вычислить значение ETag на стороне ES. Это быстрая аггрегация, выполняется прямо на шардах.
      val remoteEtagFut = for (
        nodesHashSum <- advGeoRcvrsUtil.rcvrNodesMapHashSumCached()
      ) yield {
        s""""$nodesHashSum""""
      }

      lazy val logPrefix = s"advRcvrsMapJson#${System.currentTimeMillis()}:"

      // На основе спецификации клиента выбрать алгоритм сжатия, в котором требуется получить ответ.
      val respCompAlgoOpt = request.headers
        .get(ACCEPT_ENCODING)
        .flatMap( MCompressAlgos.chooseSmallestForAcceptEncoding )

      for {
        etag <- remoteEtagFut

        // Совпадает ли Etag со значением на клиенте?
        etagMatches = request.headers
          .get(IF_NONE_MATCH)
          .contains( etag )

        resultBase <- {
          if (etagMatches) {
            LOGGER.trace(s"$logPrefix Etag match $etag, returning 304")
            Future.successful( NotModified )

          } else {
            // Запускаем сборку карты:
            val rcvrsMapRespJsonFut = _advRcvrsMapRespJsonFut( respCompAlgoOpt )

            // Значение ETag требуется рендерить в хидеры в двойных ковычках, оформляем:
            val headers0 = List(
              ETAG              -> s""""$etag"""",
              VARY              -> ACCEPT_ENCODING
            )

            rcvrsMapRespJsonFut.flatMap { srcOrCached =>
              srcOrCached.fold[Future[Result]](
                // В кэше нет, но есть подготовленный стриминг. Выполнить стримминг:
                {src =>
                  // Разрулить возможные проблемы с http<1.1. Это например nginx любит по дефолту.
                  val r0 = if (request.request.version ==* HttpProtocol.HTTP_1_0) {
                    LOGGER.warn(s"$logPrefix HTTP 1.0, streaming. Is nginx/proxy valid?")
                    Ok.sendEntity(
                      HttpEntity.Streamed(
                        data          = src,
                        contentLength = None,
                        contentType   = Some(JSON)
                      )
                    )
                  } else {
                    Ok.chunked(src)
                      .as( JSON )
                  }
                  respCompAlgoOpt.fold(r0) { respCompAlgo =>
                    r0.withHeaders(
                      (CONTENT_ENCODING -> respCompAlgo.httpContentEncoding) :: headers0: _*
                    )
                  }
                },
                // Есть ответ кэша. Возможно, что ещё не готовый. Но вернуть его как strict.
                {respData =>
                  for {
                    respBodyVar <- respData.body
                  } yield {
                    LOGGER.trace(s"$logPrefix Have cached resp: ${respData.compressAlgo}: ${respBodyVar.value.length}b")
                    Ok( respBodyVar.value )
                      .as( JSON )
                      .withHeaders(
                        (CONTENT_ENCODING -> respData.compressAlgo.httpContentEncoding) :: headers0: _*
                      )
                  }
                }
              )
            }
          }
        }

      } yield {
        resultBase
      }
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
  // TODO 2018-03-12 boopickle и bin-формат теперь только для совместимости с кривыми установленными webapp sc3. Можно её удалить ближе к запуску. Удалить .etag поле из модели выше.
  def advRcvrsMap = {
    ignoreAuth().async { implicit request =>
      // Завернуть данные в единый блоб и отправить клиенту.
      val accept = request.headers.get( ACCEPT )
      lazy val logPrefix = s"advRcvrsMap(${accept.orNull})#${System.currentTimeMillis()}:"

      for {
        respData <- _advRcvrsMapRespBoopickleFut()

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
            VARY          -> (ACCEPT_ENCODING :: Nil).mkString(", ")
          )
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
