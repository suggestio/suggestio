package controllers.sc

import _root_.util.jsa.{Js, SmRcvResp}
import io.suggest.event.subscriber.SnFunSubscriber
import io.suggest.model.n2.node.event.MNodeSaved
import models.jsm.NodeDataResp
import models.mproj.IMProjectInfo
import play.api.mvc.Result
import util.DateTimeUtil
import util.acl.AdnNodeMaybeAuth
import util.cdn.ICdnUtilDi
import util.xplay.CacheUtil
import views.html.lk.adn.node._installScriptTpl
import views.txt.sc._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.11.14 19:56
 * Description: Доступ к данным узла. В первую очередь для осуществления работы внедряемой иконки на wifi.
 */
trait ScNodeInfo
  extends ScController
  with AdnNodeMaybeAuth
  with ICdnUtilDi
  with IMProjectInfo
{

  import mCommonDi._

  /** Сколько времени кешировать скомпиленный скрипт nodeIconJsTpl. */
  private val NODE_ICON_JS_CACHE_TTL_SECONDS: Int = {
    configuration.getInt("market.node.icon.js.cache.ttl.seconds")
      .getOrElse(30)
  }

  private val NODE_ICON_JS_CACHE_CONTROL_MAX_AGE: Int = {
    configuration
      .getInt("market.node.icon.js.cache.control.max.age")
      .getOrElse {
        if (isProd)  60  else  6
      }
  }

  // Подписаться на события обновления узлов, чтобы сбрасывать кеш.
  sn.subscribe(
    subscriber = SnFunSubscriber {
      case anse: MNodeSaved =>
        val ck = nodeIconJsCacheKey(anse.nodeId)
        cache.remove(ck)
    },
    classifier = MNodeSaved.getClassifier(isCreated = Some(false))
  )

  /** Кеш-ключ для nodeIconJs. */
  private def nodeIconJsCacheKey(adnId: String) = adnId + ".nodeIconJs"

  /** Экшн, который рендерит скрипт с иконкой. Используется кеширование на клиенте и на сервере. */
  def nodeIconJs(adnId: String) = AdnNodeMaybeAuth(adnId).async { implicit request =>
    // Проверяем ETag
    val isCachedEtag = request.mnode.versionOpt
      .flatMap { vsn =>
        request.headers.get(IF_NONE_MATCH)
          .filter { etag  =>  vsn.toString == etag }
      }
      .nonEmpty
    // Проверяем Last-Modified, если ETag верен.
    val isCached = isCachedEtag && {
      request.headers.get(IF_MODIFIED_SINCE)
        .flatMap { DateTimeUtil.parseRfcDate }
        .exists { dt =>
          !(mProjectInfo.PROJECT_CODE_LAST_MODIFIED isAfter dt)
        }
    }
    if (isCached) {
      NotModified
    } else {
      val ck = nodeIconJsCacheKey(adnId)
      CacheUtil.getOrElse [Result] (ck, NODE_ICON_JS_CACHE_TTL_SECONDS) {
        val logoFut = logoUtil.getLogoOfNode( request.mnode )
        var cacheHeaders: List[(String, String)] = List(
          LAST_MODIFIED -> DateTimeUtil.rfcDtFmt.print(mProjectInfo.PROJECT_CODE_LAST_MODIFIED),
          CACHE_CONTROL -> ("public, max-age=" + NODE_ICON_JS_CACHE_CONTROL_MAX_AGE)
        )
        if (request.mnode.versionOpt.isDefined) {
          cacheHeaders  ::=  ETAG -> request.mnode.versionOpt.get.toString
        }
        for (logoOpt <- logoFut) yield {
          // TODO Добавить минификацию скомпиленного js-кода. Это снизит нагрузку на кеш (на RAM) и на сеть.
          // TODO Добавить поддержку gzip надо бы.
          // TODO Кешировать отрендеренные результаты на HDD, а не в RAM.
          Ok(nodeIconJsTpl(request.mnode, logoOpt))
            .as("text/javascript; charset=utf-8")
            .withHeaders(cacheHeaders : _*)
        }
      }
    }
  }

  /** Экшн, который выдает базовую инфу о ноде */
  def nodeData(adnId: String) = AdnNodeMaybeAuth(adnId).async { implicit request =>
    val mnode = request.mnode
    val logoCallOptFut = for(
      logoOpt <- logoUtil.getLogoOfNode( mnode )
    ) yield {
      logoOpt.map { logoImg =>
        cdnUtil.dynImg( logoImg )
      }
    }
    for {
      logoCallOpt <- logoCallOptFut
    } yield {
      val resp = NodeDataResp(
        colorOpt    = mnode.meta.colors.bg.map(_.code),
        logoUrlOpt  = logoCallOpt
      )
      cacheControlShort {
        Ok( Js(SmRcvResp(resp)) )
      }
    }
  }

  /** Рендер скрипта выдачи для указанного узла. */
  def nodeSiteScript(adnId: String) = AdnNodeMaybeAuth(adnId).apply { implicit request =>
    Ok(_installScriptTpl(request.mnode.id, mkPermanent = true))
      .as("text/javascript; charset=utf-8")
      .withHeaders(
        CACHE_CONTROL -> "public, max-age=36000"
      )
  }

}
