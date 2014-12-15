package util.acl

import models._
import play.api.mvc.{Result, Request, ActionBuilder}
import util.acl.PersonWrapper.PwOpt_t
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.08.14 10:33
 * Description: ActionBuilder для доступа к странице market/index и её производным.
 * Тут не acl, а просто common-код для рендера этой страницы, которая отображается из разных мест.
 */
object MarketIndexAccess extends ActionBuilder[MarketIndexRequest] {

  /** Надо найти узлы, которые стоит отобразить на странице как точки для размещения рекламы.
   * Это должны быть не-тестовые ресиверы, имеющие логотипы. */
  def getNodes: Future[Seq[MAdnNode]] = {
    val nodeSearchArgs = new AdnNodesSearchArgs {
      override def hasLogo = Some(true)
      override def testNode = Some(false)
      override def maxResults = controllers.Market.INDEX_NODES_LIST_LEN
      override def withAdnRights = Seq(AdnRights.RECEIVER)
    }
    MAdnNode.dynSearch(nodeSearchArgs)
  }

  override def invokeBlock[A](request: Request[A], block: (MarketIndexRequest[A]) => Future[Result]): Future[Result] = {
    val nodesFut = getNodes
    val pwOpt = PersonWrapper.getFromRequest(request)
    SioReqMd.fromPwOpt(pwOpt) flatMap { srm =>
      nodesFut flatMap { nodes =>
        val req1 = MarketIndexRequest(nodes, pwOpt, request, srm)
        block(req1)
      }
    }
  }
}

final case class MarketIndexRequest[A](displayNodes: Seq[MAdnNode], pwOpt: PwOpt_t, request: Request[A], sioReqMd: SioReqMd)
  extends AbstractRequestWithPwOpt[A](request)
  with SioRequestHeader
