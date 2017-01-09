package io.suggest.xadv.ext.js.fb.c

import io.suggest.xadv.ext.js.fb.c.hi.Fb
import io.suggest.xadv.ext.js.fb.m.{FbNodeTypes, FbNodeInfoArgs, FbTarget}
import io.suggest.xadv.ext.js.runner.m.IMExtTarget
import io.suggest.xadv.ext.js.runner.m.ex.UnknownTgUrlException
import org.scalajs.dom

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.15 15:49
 * Description: Утиль для facebook-контроллера.
 */
object FbUtil {

  /**
   * Узнать тип указанной ноды через API.
   * @param fbId id узла.
   * @return None если не удалось определить id узла.
   *         Some с данными по цели, которые удалось уточнить. Там МОЖЕТ БЫТЬ id узла.
   */
  def getNodeType(fbId: String)(implicit ec: ExecutionContext): Future[Option[FbTarget]] = {
    val args = FbNodeInfoArgs(
      id        = fbId,
      fields    = FbNodeInfoArgs.FIELDS_ONLY_ID,
      metadata  = true
    )
    Fb.getNodeInfo(args)
      .map { resp =>
        val ntOpt = resp.metadata
          .flatMap(_.nodeType)
          .orElse {
            // Если всё еще нет типа, но есть ошибка, то по ней бывает можно определить тип.
            resp.error
              .filter { e => e.code contains 803 }    // Ошибка 803 говорит, что у юзера нельзя сдирать инфу.
              .map { _ => FbNodeTypes.User }
          }
        if (ntOpt.isEmpty)
          dom.console.warn("Unable to get node type from getNodeInfo(%s) resp %s", args.toString, resp.toString)
        val res = FbTarget(
          nodeId        = resp.nodeId.getOrElse(fbId),
          nodeType  = ntOpt
        )
        Some(res)
      // recover() для подавления любых возможных ошибок.
      }.recover {
        case ex: Throwable =>
          dom.console.warn("Failed to getNodeInfo(%s) or parse result: %s %s", args.toString, ex.getClass.getSimpleName, ex.getMessage)
          None
      }
  }

  /** Определение типа целевого таргета. */
  def detectTgNodeType(target: IMExtTarget)(implicit ec: ExecutionContext): Future[FbTarget] = {
    // Первый шаг - определяем по узлу данные.
    val tgInfoOpt = FbTarget.fromUrl( target.tgUrl )
    tgInfoOpt match {
      // Неизвестный URL. Завершаемся.
      case None =>
        Future failed new UnknownTgUrlException
      case Some(info) =>
        if (info.nodeType.isEmpty) {
          // id узла есть, но тип не известен. Нужно запустить определение типа указанной цели.
          getNodeType(info.nodeId)
            .map { _ getOrElse info }
        } else {
          Future successful info
        }
    }
  }

}
