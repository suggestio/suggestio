package io.suggest.sjs.common.tags.search

import io.suggest.common.tags.search.MTagsFound
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model._
import io.suggest.tags.MTagsSearchQs
import io.suggest.xplay.json.PlayJsonSjsUtil
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.12.16 14:53
  * Description: Api для связи с сервером на тему поиска тегов.
  */
trait ITagsApi {

  /** Метод запроса к серверу для поиска тегов. */
  def tagsSearch(args: MTagsSearchQs): Future[MTagsFound]

}


/** Реализация [[ITagsApi]] через XHR с бинарщиной в ответе. */
trait TagsHttpApiImpl extends ITagsApi {

  /** Функция-генератор роуты для поиска тегов на сервере. */
  protected def _tagsSearchRoute: js.Dictionary[js.Any] => Route

  override def tagsSearch(args: MTagsSearchQs): Future[MTagsFound] = {
    val req = HttpReq.routed(
      route = _tagsSearchRoute(
        PlayJsonSjsUtil.toNativeJsonObj(
          Json.toJsObject(args) ) ),
      data  = HttpReqData(
        headers  = HttpReqData.headersBinaryAccept,
        respType = HttpRespTypes.ArrayBuffer
      )
    )
    HttpClient.execute( req )
      .respAuthFut
      .successIf200
      .unJson[MTagsFound]
  }

}
