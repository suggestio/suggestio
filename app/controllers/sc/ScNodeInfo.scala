package controllers.sc

import _root_.util.jsa.{Js, SmRcvResp}
import controllers.SioControllerUtil
import SioControllerUtil.PROJECT_CODE_LAST_MODIFIED
import io.suggest.event.subscriber.SnFunSubscriber
import io.suggest.event.{AdnNodeSavedEvent, SNStaticSubscriber}
import models.jsm.NodeDataResp
import play.api.Play, Play.{current, configuration}
import play.api.cache.Cache
import util.DateTimeUtil
import util.acl.AdnNodeMaybeAuth
import util.cdn.CdnUtil
import views.html.lk.adn.node._installScriptTpl
import views.txt.sc._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.11.14 19:56
 * Description: Доступ к данным узла. В первую очередь для осуществления работы внедряемой иконки на wifi.
 */
trait ScNodeInfo extends ScController with SNStaticSubscriber {

  /** Сколько времени кешировать скомпиленный скрипт nodeIconJsTpl. */
  private val NODE_ICON_JS_CACHE_TTL_SECONDS = configuration.getInt("market.node.icon.js.cache.ttl.seconds") getOrElse 30
  private val NODE_ICON_JS_CACHE_CONTROL_MAX_AGE: Int = configuration.getInt("market.node.icon.js.cache.control.max.age") getOrElse {
    if (Play.isProd)  60  else  6
  }

  /** Кеш-ключ для nodeIconJs. */
  private def nodeIconJsCacheKey(adnId: String) = adnId + ".nodeIconJs"

  /** Экшн, который рендерит скрипт с иконкой. Используется кеширование на клиенте и на сервере. */
  def nodeIconJs(adnId: String) = AdnNodeMaybeAuth(adnId).apply { implicit request =>
    // Проверяем ETag
    val isCachedEtag = request.adnNode.versionOpt
      .flatMap { vsn =>
        request.headers.get(IF_NONE_MATCH)
          .filter { etag  =>  vsn.toString == etag }
      }
      .nonEmpty
    // Проверяем Last-Modified, если ETag верен.
    val isCached = isCachedEtag && {
      request.headers.get(IF_MODIFIED_SINCE)
        .flatMap { DateTimeUtil.parseRfcDate }
        .exists { dt => !(PROJECT_CODE_LAST_MODIFIED isAfter dt)}
    }
    if (isCached) {
      NotModified
    } else {
      val ck = nodeIconJsCacheKey(adnId)
      Cache.getOrElse(ck, expiration = NODE_ICON_JS_CACHE_TTL_SECONDS) {
        var cacheHeaders: List[(String, String)] = List(
          CONTENT_TYPE  -> "text/javascript; charset=utf-8",
          LAST_MODIFIED -> DateTimeUtil.rfcDtFmt.print(PROJECT_CODE_LAST_MODIFIED),
          CACHE_CONTROL -> ("public, max-age=" + NODE_ICON_JS_CACHE_CONTROL_MAX_AGE)
        )
        if (request.adnNode.versionOpt.isDefined) {
          cacheHeaders  ::=  ETAG -> request.adnNode.versionOpt.get.toString
        }
        // TODO Добавить минификацию скомпиленного js-кода. Это снизит нагрузку на кеш (на RAM) и на сеть.
        // TODO Добавить поддержку gzip надо бы.
        // TODO Кешировать отрендеренные результаты на HDD, а не в RAM.
        Ok(nodeIconJsTpl(request.adnNode))
          .withHeaders(cacheHeaders : _*)
      }
    }
  }

  /** Экшн, который выдает базовую инфу о ноде */
  def nodeData(adnId: String) = AdnNodeMaybeAuth(adnId).apply { implicit request =>
    val node = request.adnNode
    val logoSrcOpt = node.logoImgOpt map { logo_src =>
      CdnUtil.dynImg(logo_src.filename)
    }
    val resp = NodeDataResp(colorOpt = node.meta.color, logoUrlOpt = logoSrcOpt)
    cacheControlShort {
      Ok( Js(SmRcvResp(resp)) )
    }
  }

  /** Рендер скрипта выдачи для указанного узла. */
  def nodeSiteScript(adnId: String) = AdnNodeMaybeAuth(adnId).apply { implicit request =>
    Ok(_installScriptTpl(request.adnNode.id, mkPermanent = true))
      .withHeaders(
        CONTENT_TYPE  -> "text/javascript; charset=utf-8",
        CACHE_CONTROL -> "public, max-age=36000"
      )
  }

  /** Карта статической подписки контроллера на некоторые события:
    * - Уборка из кеша рендера nodeIconJs. */
  abstract override def snMap = {
    // Нужно чистить кеш nodeIconJs при обновлении узлов.
    val classifier = AdnNodeSavedEvent.getClassifier(isCreated = Some(false))
    val subscriber = SnFunSubscriber {
      case anse: AdnNodeSavedEvent =>
        val ck = nodeIconJsCacheKey(anse.adnId)
        Cache.remove(ck)
    }
    classifier -> Seq(subscriber) :: super.snMap
  }

}
