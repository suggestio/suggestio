package io.suggest.sys.mdr.c

import io.suggest.proto.HttpConst
import io.suggest.routes.routes
import io.suggest.sjs.common.xhr.Xhr
import org.scalajs.dom.ext.AjaxException
import japgolly.univeq._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.xhr.ex.XhrUnexpectedRespStatusException
import io.suggest.sys.mdr.{MMdrResolution, MNodeMdrInfo, MdrSearchArgs}
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
  def nextMdrInfo(args: MdrSearchArgs): Future[Option[MNodeMdrInfo]]

  /** Запрос на сервер с резолюцией модератора. */
  def doMdr(mdrRes: MMdrResolution): Future[_]

}


class SysMdrApiXhrImpl extends ISysMdrApi {

  /** Сервер отвечает 204, если нечего возвращать в ответе при положительном исходе запроса. */
  private def _isOkNothingInteresting(status: Int): Boolean =
    status ==* HttpConst.Status.NO_CONTENT

  private val _recover204ToNone: PartialFunction[Throwable, None.type] = {
    case ex: XhrUnexpectedRespStatusException if _isOkNothingInteresting(ex.xhr.status) =>
      None
    case ex: AjaxException if _isOkNothingInteresting(ex.xhr.status) =>
      None
  }


  override def nextMdrInfo(args: MdrSearchArgs): Future[Option[MNodeMdrInfo]] = {
    val route = routes.controllers.SysMdr.nextMdrInfo(
      args = PlayJsonSjsUtil.toNativeJsonObj( Json.toJsObject(args) )
    )
    Xhr.unJsonResp[MNodeMdrInfo] {
      Xhr.requestJsonText(route)
    }
      .map( Some.apply )
      .recover( _recover204ToNone )
  }


  override def doMdr(mdrRes: MMdrResolution): Future[_] = {
    val route = routes.controllers.SysMdr.doMdr(
      mdrRes = PlayJsonSjsUtil.toNativeJsonObj( Json.toJsObject(mdrRes) )
    )
    // TODO Десериализовать/обработать ответ.
    Xhr.requestJsonText( route )
      .recover( _recover204ToNone )
  }

}
