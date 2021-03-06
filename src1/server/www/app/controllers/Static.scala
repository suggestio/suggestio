package controllers

import javax.inject.Inject
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{FlowShape, Graph, Materializer}
import akka.stream.scaladsl.{Compression, Flow, Keep, Sink, Source}
import akka.util.ByteString
import io.suggest.brotli.BrotliUtil
import io.suggest.compress.{MCompressAlgo, MCompressAlgos}
import io.suggest.ctx.MCtxId
import io.suggest.es.model.EsModel
import io.suggest.n2.node.{MNode, MNodes}
import io.suggest.primo.Var
import io.suggest.stat.m.{MComponents, MDiag}
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.playx.CacheApiUtil
import io.suggest.proto.http.model.MCsrfToken
import io.suggest.sec.util.Csrf
import io.suggest.streams.{ByteStringsChunker, StreamsUtil}
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.{Context, ContextUtil}
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
import views.html.static._
import japgolly.univeq._
import play.api.{Application, Configuration}
import play.api.http.{HeaderNames, HttpEntity, HttpErrorHandler, HttpProtocol}
import play.filters.csrf.CSRF
import play.twirl.api.Xml
import views.txt.static.RobotsTxtTpl
import io.suggest.playx._

import java.net.URI
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
final class Static @Inject() (
                               sioControllerApi                : SioControllerApi,
                             )
  extends MacroLogsImpl
{

  import sioControllerApi._

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val ignoreAuth = injector.instanceOf[IgnoreAuth]
  private lazy val corsUtil = injector.instanceOf[CorsUtil]
  private lazy val siteMapUtil = injector.instanceOf[SiteMapUtil]
  private lazy val isAuth = injector.instanceOf[IsAuth]
  private lazy val advGeoRcvrsUtil = injector.instanceOf[AdvGeoRcvrsUtil]
  private lazy val statUtil = injector.instanceOf[StatUtil]
  private lazy val streamsUtil = injector.instanceOf[StreamsUtil]
  private lazy val aclUtil = injector.instanceOf[AclUtil]
  private lazy val cspUtil = injector.instanceOf[CspUtil]
  private lazy val isSuOrDevelOr404 = injector.instanceOf[IsSuOrDevelOr404]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val maybeAuth = injector.instanceOf[MaybeAuth]
  private lazy val canOpenWsChannel = injector.instanceOf[CanOpenWsChannel]
  private lazy val wsChannelActors = injector.instanceOf[WsChannelActors]
  private lazy val assets = injector.instanceOf[Assets]
  private lazy val contextUtil = injector.instanceOf[ContextUtil]
  private lazy val cacheApiUtil = injector.instanceOf[CacheApiUtil]
  private lazy val csrf = injector.instanceOf[Csrf]
  private lazy val configuration = injector.instanceOf[Configuration]
  private lazy val errorHandler = injector.instanceOf[HttpErrorHandler]
  private lazy val current = injector.instanceOf[Application]
  private lazy val marketOfferoTpl = injector.instanceOf[MarketOfferoTpl]
  implicit private lazy val mat = injector.instanceOf[Materializer]
  implicit private lazy val actorSystem = injector.instanceOf[ActorSystem]

  private lazy val robotsTxtTpl = injector.instanceOf[RobotsTxtTpl]

  private def _IGNORE_CSP_REPORS_CONF_KEY = "csp.reports.ignore"

  /** Полностью игнорить CSP-отчёты? [false] */
  private lazy val _IGNORE_CSP_REPORTS = {
    configuration
      .getOptional[Boolean]( _IGNORE_CSP_REPORS_CONF_KEY )
      .getOrElseFalse
  }


  /**
   * Страница с политикой приватности.
   * @return 200 Ok и много букв.
   */
  def privacyPolicy() = maybeAuth() { implicit request =>
    Ok( privacyPolicyTpl() )
      .cacheControl( 3600 )
  }

  def offero() = maybeAuth() { implicit request =>
    Ok( marketOfferoTpl() )
      .cacheControl( 3600 )
  }

  /** Содержимое проверочного попап-окна. */
  def popupCheckContent() = maybeAuth() { implicit request =>
    Ok( popups.popupCheckTpl() )
      .cacheControl( 86400 )
  }

  /**
   * Доступ к привилегированным ассетам: js.map и прочие сорцы.
   * @param path Путь.
   * @param asset filename.
   * @return Экшен раздачи ассетов с сильно урезанным кешированием на клиенте.
   */
  def vassetsSudo(path: String, asset: Assets.Asset) = isSuOrDevelOr404().async { implicit request =>
    // Для привелегированных ассетов нужно запретить промежуточные кеширования.
    for {
      res <- assets.versioned(path, asset)(request)
    } yield {
      val ttl = if (current.mode.isProd) 300 else 10
      res.withHeaders(CACHE_CONTROL -> s"private, max-age=$ttl")
    }
  }

  def assetsSudo(path: String, asset: Assets.Asset) = vassetsSudo(path, asset)


  /**
   * Экшен для скрытого продления сессии в фоне. Может дергаться в js'ом незаметно.
   * @return 204 No Content - всё ок.
   *         Другой код - сессия истекла.
   */
  def keepAliveSession() = isAuth() { implicit request =>
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
      // Нет никакой надобности отвечать html-страницами, т.к. на CSP-экшены никто кроме браузеров не обращается. Но для надёжности, ответим через обычный путь:
      errorHandler.onClientError(req0, UNSUPPORTED_MEDIA_TYPE, "CSP-report expected")
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
  def handleCspReport() = {
    lazy val logPrefix = s"cspReportHandler[${System.currentTimeMillis()}]:"
    if (_IGNORE_CSP_REPORTS) {
      maybeAuth() (_cspJsonBp) { implicit request =>
        LOGGER.trace(s"$logPrefix Ignored CSP-report from [${request.remoteClientAddress}] because conf.: '${_IGNORE_CSP_REPORS_CONF_KEY}' = ${_IGNORE_CSP_REPORTS}")
        NoContent
      }

    } else {
      // bruteForceProtect нельзя. CSP часто сыпляться пачками, и это приведёт к блэклистингу кучи вызовов API к IP.
      maybeAuth().async( _cspJsonBp ) { implicit request =>
        // Залить ошибку в MStat.
        val requestBodyStr = request.body.toString()
        request.body.validate( cspUtil.WRAP_REPORT_READS ).fold(
          // Ошибка парсинга JSON-тела. Вообще, это обычно неправильно.
          {violations =>
            val msg = s"Invalid JSON: ${violations.mkString(", ")}"
            LOGGER.warn(s"$logPrefix $msg\n---------\n${Json.prettyPrint(request.body)}")
            errorHandler.onClientError(request, BAD_REQUEST, msg)
          },

          // Всё ок распарсилось.
          {cspViol =>
            // Сам результат парсинга не особо важен, это скорее контроль контента.
            val userSaOptFut = statUtil.userSaOptFutFromRequest()
            implicit val _ctx = implicitly[Context]

            for {
              _userSaOpt <- userSaOptFut
            } yield {
              lazy val stat2 = new statUtil.Stat2 {
                override def logMsg = Some("CSP-report")
                override def ctx = _ctx
                override def uri = Some( cspViol.documentUri )
                override def components = {
                  MComponents.CSP :: MComponents.Error :: super.components
                }
                override def statActions = Nil
                override def userSaOpt = _userSaOpt

                override def diag = {
                  if (statUtil.SAVE_GARBAGE_TO_MSTAT)
                    MDiag(
                      message = Some( requestBodyStr )
                    )
                  else
                    MDiag.empty
                }
              }

              // Check, if CSP report host related.
              val reportedHostname = URI.create(cspViol.documentUri).getHost.toLowerCase
              val isPossiblyUnrelated = contextUtil.SUGGESTIO_DOMAINS contains reportedHostname

              // Принудительно НЕ сохраняем статистику, т.к. это требует системы фильтрации и дедубликации мегатонн статистики.
              //r <- statUtil.maybeSaveGarbageStat( stat2, logTail =  )
              val r = statUtil.dontSaveStat(
                stat2,
                infoLogLevel = isPossiblyUnrelated,
                logTail = Json.prettyPrint(request.body)
              )

              LOGGER.trace( s"$logPrefix Done: CSP-report#$r from ${_ctx.request.remoteClientAddress}" )
              NoContent
            }
          }
        )
      }
    }
  }


  /** Контейнер для данных ответа сервера на тему карты ресиверов.
    * Абстрагирует всякие особенности json/binary сериализаций от итогов работы.
    *
    * @param contentType Тип содержимого ответа.
    * @param gzip gzip-пожатый ответ.
    * @param brotli Фьючрес с brotli-пожатый ответ.
    */
  private case class MAdvRcvrsMapRespData(
                                           contentType   : String,
                                           gzip          : Future[Var[ByteString]],
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
          import esModel.api._
          import mNodes.Implicits._
          import streamsUtil.Implicits._

          // Организуем реактивную обработку нод с генерацией несжатых byte chunks:
          val NODES_PER_CHUNK = 30
          val chunkedUncompressedSrc = advGeoRcvrsUtil
            .withNodeLocShapes(
              advGeoRcvrsUtil.nodesAdvGeoPropsSrc(
                mNodes.source[MNode](
                  advGeoRcvrsUtil
                    .onMapRcvrsSearch(NODES_PER_CHUNK)
                    .toEsQuery
                ),
              )
            )
            // JSON-рендер каждого item'а:
            .map { case (_, data) =>
              Json.toJson(data)
            }
            .jsValuesToJsonArrayByteStrings
            // Компрессоры захлёбываются? Это проблемы компрессоров)) Стараемся разогнать генерацию данных на максимум ценой некоторой RAM.
            .conflate(_ ++ _)
            // Нормализовать размеры ByteString'ов для входа компрессоров. Для Gzip это влияет на коэфф.сжатия.
            .via( ByteStringsChunker(8192) )

          val byteStringSink = streamsUtil.byteStringAccSink
            // Запускать в фоне compact, который подменит исходное неоптимизированное значение ByteString.
            .asyncCompactedByteString

          // Дедубликация кода сборки одного sink'а любого сжатия
          def __compressSink( compressFlow: Graph[FlowShape[ByteString, ByteString], NotUsed]): Sink[ByteString, Future[Var[ByteString]]] = {
            Flow[ByteString]
              .via( compressFlow )
              .toMat( byteStringSink )(Keep.right)
          }

          lazy val logPrefix = s"_advRcvrsMapRespJsonFut#${System.currentTimeMillis()}:"

          // Общий код вызова финального mapMaterialzedValue(), который сохранит всё в кэш и вернёт нормализованное значение.
          def __mapMaterialized(gzipFut    : Future[Var[ByteString]],
                                brotliFut  : Future[Var[ByteString]]): MAdvRcvrsMapRespData = {
            val r = MAdvRcvrsMapRespData(JSON, gzipFut, Some(brotliFut))
            cachedPromise.success( r )

            if (LOGGER.underlying.isTraceEnabled) {
              for (gzipped <- gzipFut)
                LOGGER.trace(s"$logPrefix Total gzipped size: ${gzipped.value.length} bytes")
              for (brotlied <- brotliFut)
                LOGGER.trace(s"$logPrefix Total brotli size: ${brotlied.value.length} bytes")
            }

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

          // Залоггировать возможные ошибки:
          val respSrcIfFail = respSrc.mapError { case ex =>
            LOGGER.error(s"$logPrefix Sourcing failed.", ex)
            ex
          }
          Left(respSrcIfFail)
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
    * @param nodesHashSum Ключ для долгосрочного кэширования уровня URL (вместо ETag).
    *
    * @return Ok + Streamed/Chunked (заполнения кэша)
    *         Ok + Strict (извлечение из кэша)
    */
  def advRcvrsMapJson(nodesHashSum: Int) = {
    ignoreAuth().async { implicit request =>
      // Быстро вычислить значение ETag на стороне ES. Это быстрая аггрегация, выполняется прямо на шардах.
      val realNodesHashSumFut = advGeoRcvrsUtil.rcvrNodesMapHashSumCached()

      lazy val logPrefix = s"advRcvrsMapJson#${System.currentTimeMillis()}:"

      for {
        realNodesHashSum <- realNodesHashSumFut
        etagNoQuotes = realNodesHashSum.toString

        resultBase <- {
          // Если URL не совпадает с вычисленным значением, то сразу можно редиректить на правильную ссылку
          if ( nodesHashSum !=* realNodesHashSum ) {
            LOGGER.warn(s"$logPrefix Unexpected caching: $nodesHashSum, but $etagNoQuotes expected, redirection...")
            val rdr = Redirect( routes.Static.advRcvrsMapJson( realNodesHashSum ) )
            Future.successful(rdr)

          } else if (
            // Совпадает ли Etag со значением на клиенте?
            request.headers
              .get(IF_NONE_MATCH)
              .exists { ifNoneMatch =>
                // Значение If-None-Match иногда приходит с клиента без кавычек, но обычно с. Сравниваем как подстроки, наврядли тут будут ложные срабатывания.
                ifNoneMatch contains etagNoQuotes
              }
          ) {
            LOGGER.trace(s"$logPrefix Etag match $etagNoQuotes, returning 304")
            Future.successful( NotModified )

          } else {
            // На основе спецификации клиента выбрать алгоритм сжатия, в котором требуется получить ответ.
            val respCompAlgoOpt = request.headers
              .get(ACCEPT_ENCODING)
              .flatMap( MCompressAlgos.chooseSmallestForAcceptEncoding )

            // Запускаем сборку карты:
            val rcvrsMapRespJsonFut = _advRcvrsMapRespJsonFut( respCompAlgoOpt )

            // Если ссылка с кэшем, то допускам долгое кэширование.

            val okHeaders = List(
              // Значение ETag требуется рендерить в хидеры в двойных ковычках, оформляем:
              ETAG              -> s""""$etagNoQuotes"""",
              VARY              -> ACCEPT_ENCODING,
              // Наврядли есть смысл делать очень длинный TTL: карта узлов может обновляться сто раз на дню.
              CACHE_CONTROL     -> s"public, max-age=${30.days.toSeconds}, immutable"
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
                      (CONTENT_ENCODING -> respCompAlgo.httpContentEncoding) :: okHeaders: _*
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
                        (CONTENT_ENCODING -> respData.compressAlgo.httpContentEncoding) :: okHeaders: _*
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
    canOpenWsChannel.can(ctxId) match {
      // Разрешаем коннекшен
      case Some(mrh) =>
        LOGGER.trace(s"$logPrefix Opening connection for user#${mrh.user.personIdOpt.orNull} ctx#${ctxId.key}...")
        val aFlow = ActorFlow.actorRef(
          props = { out =>
            val args = MWsChannelActorArgs(out, ctxId)
            wsChannelActors.props(args)
          }
        )
        val r = Right(aFlow)
        Future.successful(r)

      // Не разрешён коннект: чтоо-то нет так с ctxId.
      case None =>
        val ctxIdStr = ctxId.toString
        LOGGER.warn(s"$logPrefix not allowed: $ctxIdStr")
        errorHandler.onClientError(rh, FORBIDDEN, s"Forbidden: $ctxIdStr")
          .map(Left.apply)
    }
  }


  /**
    * Реакция на options-запрос, хидеры выставит CORS-фильтр, подключенный в Global.
    * @param path Путь, к которому запрошены опшыны.
    * @return
    */
  def corsPreflight(path: String) = maybeAuth().async { implicit request =>
    val isEnabled = corsUtil.IS_ENABLED

    if (
      isEnabled &&
      request.headers.get( HeaderNames.ACCESS_CONTROL_REQUEST_METHOD ).nonEmpty
    ) {
      // Кэшировать ответ на клиенте для ускорения работы системы. TODO Увеличить значение на неск.порядков:
      LOGGER.trace( s"corsPreflight($path): 200 OK to ${request.remoteClientAddress}\n UA: ${request.headers.get(USER_AGENT)}\n X-Requested-With: ${request.headers.get(X_REQUESTED_WITH)}" )
      Ok.cacheControl( 300 )

    } else {
      val body = if (isEnabled) "Missing nessesary CORS headers" else "CORS is disabled"
      errorHandler.onClientError(request, NOT_FOUND, body)
    }
  }



  /** Время кеширования /sitemap.xml ответа на клиенте. */
  private def SITEMAP_XML_CACHE_TTL_SECONDS = if (current.mode.isDev) 1 else 120


  /**
    * Раздача сайт-мапы.
    * @return sitemap, генерируемый поточно с очень минимальным потреблением RAM.
    */
  def siteMapXml() = ignoreAuth() { implicit request =>
    import views.xml.static.sitemap._

    implicit val ctx = implicitly[Context]

    // Собираем асинхронный неупорядоченный источник sitemap-ссылок:
    val urls = siteMapUtil
      .sitemapUrlsSrc()( ctx )
      // Рендерим каждую ссылку в текст
      .map( _urlTpl.apply )
      .recover { case ex: Throwable =>
        LOGGER.error("siteMapXml: Unable to render url", ex)
        Xml(s"<!-- Stream error occured: ${ex.getClass.getName} -->")
      }

    // Рендерим ответ: сначала заголовок sitemaps.xml:
    val respBody2: Source[Xml, _] = {
      Source
        .single( beforeUrlsTpl() )
        // Затем тело, содержащее ссылки...
        .concat( urls )
        // Футер sitemaps.xml:
        .concat {
          Source.single( afterUrlsTpl() )
        }
    }

    // Возвращаем chunked-ответ с XML.
    Ok.chunked(respBody2)
      .withHeaders(
        CACHE_CONTROL -> s"public, max-age=$SITEMAP_XML_CACHE_TTL_SECONDS"
      )
  }


  /** Время кеширования /robots.txt ответа на клиенте. */
  private def ROBOTS_TXT_CACHE_TTL_SECONDS: Int =
    if (current.mode.isDev) 5 else 120

  /** Раздача содержимого robots.txt. */
  def robotsTxt() = ignoreAuth().async { implicit request =>
    implicit val ctx = implicitly[Context]
    for {
      domain3pNodeOpt <- ctx.domainNode3pOptFut
    } yield {
      val hostPort3pOpt = domain3pNodeOpt.map(_ => request.host)
      Ok( robotsTxtTpl(hostPort3pOpt)(ctx) )
        .withHeaders(
          //CONTENT_TYPE  -> "text/plain; charset=utf-8",
          CACHE_CONTROL -> s"public, max-age=$ROBOTS_TXT_CACHE_TTL_SECONDS"
        )
    }
  }


  /** Свободная раздача CSRF-токена для нужд некоторых js-форм. */
  def csrfToken() = csrf.AddToken {
    Action { implicit request =>
      val playTok = CSRF.getToken.get
      val sioTok = MCsrfToken(
        qsKey = playTok.name,
        value = playTok.value,
      )
      Ok( Json.toJson( sioTok ) )
    }
  }

}
