package io.suggest.sys.mdr.c

import io.suggest.proto.HttpConst
import io.suggest.routes.routes
import io.suggest.sjs.common.xhr.Xhr
import japgolly.univeq._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.xhr.ex.XhrFailedException
import io.suggest.sys.mdr.{MMdrNextResp, MMdrResolution, MdrSearchArgs}
import io.suggest.xplay.json.PlayJsonSjsUtil
import play.api.libs.json.Json

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.10.18 22:34
  * Description: Серверное API для SysMdr-контроллера.
  */
trait ISysMdrApi {

  /** Запрос на сервер за данными для модерации. */
  def nextMdrInfo(args: MdrSearchArgs): Future[MMdrNextResp]

  /** Запрос на сервер с резолюцией модератора. */
  def doMdr(mdrRes: MMdrResolution): Future[_]

  /** Запрос на ремонт узла. */
  def fixNode(nodeId: String): Future[_]

}


class SysMdrApiXhrImpl extends ISysMdrApi {

  /** Сервер отвечает 204, если нечего возвращать в ответе при положительном исходе запроса. */
  private def _isOkNothingInteresting(status: Int): Boolean =
    status ==* HttpConst.Status.NO_CONTENT

  private val _recover204ToNone: PartialFunction[Throwable, None.type] = {
    case ex: XhrFailedException if _isOkNothingInteresting(ex.xhr.status) =>
      None
  }


  override def nextMdrInfo(args: MdrSearchArgs): Future[MMdrNextResp] = {
    val route = routes.controllers.SysMdr.nextMdrInfo(
      args    = PlayJsonSjsUtil.toNativeJsonObj( Json.toJsObject(args) )
    )
    Xhr.unJsonResp[MMdrNextResp] {
      Xhr.requestJsonText(route)
    }
  }


  override def doMdr(mdrRes: MMdrResolution): Future[_] = {
    val route = routes.controllers.SysMdr.doMdr(
      mdrRes = PlayJsonSjsUtil.toNativeJsonObj( Json.toJsObject(mdrRes) )
    )
    // TODO Десериализовать/обработать ответ.
    Xhr.requestJsonText( route )
      .recover( _recover204ToNone )
  }


  override def fixNode(nodeId: String): Future[_] = {
    val route = routes.controllers.SysMdr.fixNode(
      nodeId = nodeId
    )
    Xhr.successIfStatus( HttpConst.Status.NO_CONTENT ) {
      Xhr.send(route)
    }
  }

}
