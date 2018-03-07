package controllers

import javax.inject.{Inject, Singleton}

import akka.util.ByteString
import controllers.cstatic.{CorsPreflight, RobotsTxt, SiteMapsXml}
import io.suggest.compress.MCompressAlgos
import io.suggest.ctx.{MCtxId, MCtxIds}
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.pick.PickleUtil
import io.suggest.sec.csp.CspViolationReport
import io.suggest.stat.m.{MComponents, MDiag}
import io.suggest.util.CompressUtilJvm
import models.mctx.Context
import models.mproj.ICommonDi
import play.api.libs.json.JsValue
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


  /** Обсчитать данные карты узлов с использование кэширования.
    *
    * @return Фьючерс с gzip-ответом и значением etag.
    */
  private def _advRcvrsMapRespFut(): Future[(ByteString, Int)] = {
    // Собрать и оформить данные по всем узлам, с использованием кэша.
    // Операция ресурсоёмкая, поэтому кэш обязателен.
    // TODO После портирования назад на JSON, надо будет akka-stream параллельно развести на gzip и brotli компрессоры и на хэшировалку. И опять же, кэшируя результат.
    cache.getOrElseUpdate("advGeoNodesSrc", expiration = 10.seconds) {
      val startedAtMs = System.currentTimeMillis()
      for {
        nodesRespMap <- advGeoRcvrsUtil.rcvrNodesMap(
          advGeoRcvrsUtil.onMapRcvrsSearch(30)
        )
        nodesResp = MGeoNodesResp(
          nodes = nodesRespMap.values
        )
        checksumFut = Future {
          nodesRespMap.hashCode()
        }
        bbuf = PickleUtil.pickle( nodesResp )
        gzippedBytea = GzipCompressConv.convert( bbuf )
        checksum <- checksumFut
      } yield {
        // Завернуть в ByteString.
        LOGGER.trace(s"_advRcvrsMapRespFut#${System.currentTimeMillis()}: Compiled resp with ${nodesResp.nodes.size} nodes, gzipped into ${gzippedBytea.length}bytes, checksum=$checksum, took=${System.currentTimeMillis() - startedAtMs} ms.")
        val gzipBytes = ByteString.fromArrayUnsafe( gzippedBytea )

        (gzipBytes, checksum)
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
      for {
        (gzipBytes, checksum) <- _advRcvrsMapRespFut()
      } yield {
        val etag = checksum.toString

        // Совпадает ли Etag со значением на клиенте?
        val etagMatches = request.headers
          .get(IF_NONE_MATCH)
          .contains( etag )

        val resultBase = if (etagMatches) {
          LOGGER.trace(s"advRcvrsMap#${System.currentTimeMillis()}: Etag match $etag, returning 304")
          NotModified
        } else {
          // Тут не проверяется Accept-Encoding. Наврядли есть клиенты без поддержки gzip.
          Ok( gzipBytes )
            .withHeaders(CONTENT_ENCODING -> MCompressAlgos.Gzip.httpContentEncoding)
        }

        resultBase
          // TODO Для 304-ответа тоже надо Etag и Cache-control?? Или только для 200 ok?
          .withHeaders(
            CACHE_CONTROL -> "public, max-age=20",
            ETAG          -> etag
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
