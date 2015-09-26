package models.usr

import io.suggest.model.n2.edge.{MPredicates, MEdge}
import util.PlayMacroLogsDyn
import util.acl.AbstractRequestWithPwOpt
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.event.SiowebNotifier.Implicts.sn

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.09.15 16:22
 * Description: Утиль для упрощенной работы с edge'ами, относящимися к юзерам.
 */
object MUserEdgeUtil extends PlayMacroLogsDyn {

  /**
   * Создать и сохранить edge юзера-создателя.
   * @param nodeId id узла, который был создан текущим юзером.
   * @param request HTTP-запрос от текущего юзера.
   * @return Фьючерс с id созданного ребра.
   */
  def saveCreatorEdge(nodeId: String)(implicit request: AbstractRequestWithPwOpt[_]): Future[String] = {
    lazy val logPrefix = s"saveCreatorEdge($nodeId):"
    val pred = MPredicates.CreatorOf
    request.pwOpt.fold[Future[String]] {
      val msg = s"$logPrefix Cannot save person edge `$pred` for anonymous user"
      LOGGER.warn(msg)
      Future failed new NoSuchElementException(msg)
    } { pw =>
      val fut = MEdge(pw.personId, pred, nodeId)
        .save
      fut.onFailure { case ex =>
        LOGGER.error(s"$logPrefix Create edge failed: $nodeId -> $pred -> ${pw.personId}", ex)
      }
      fut
    }
  }

}
