package util.acl

import io.suggest.di.{IExecutionContext, IEsClient}
import models._
import models.req.SioReqMd
import util.di.{IErrorHandler, INodeCache}
import scala.concurrent.Future
import util.acl.PersonWrapper.PwOpt_t
import play.api.mvc._
import play.api.mvc.Result

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 10:58
 * Description: Проверка наличия не-админского доступа к узлу.
 * Такое бывает, когда другой магазин пытается зайти в ЛК третьего лица.
 */
trait AdnNodeAccess
  extends IsAdnNodeAdminUtilCtl
  with IExecutionContext
  with IEsClient
  with OnUnauthUtilCtl
  with INodeCache
  with IErrorHandler
{

  /** Доступ к узлу, к которому НЕ обязательно есть права на админство. */
  sealed trait AdnNodeAccessBase
    extends ActionBuilder[RequestForAdnNode]
    with IsAdnNodeAdminUtil
    with OnUnauthUtil
  {

    def adnId: String

    def povAdnIdOpt: Option[String]

    override def invokeBlock[A](request: Request[A], block: (RequestForAdnNode[A]) => Future[Result]): Future[Result] = {
      PersonWrapper.getFromRequest(request) match {
        case pwOpt @ Some(pw) =>
          val povAdnNodeOptFut = povAdnIdOpt.fold
            { Future successful Option.empty[MNode] }
            { povAdnId => isAdnNodeAdmin(povAdnId, pwOpt) }
          val adnNodeOptFut = mNodeCache.getById(adnId)
          checkAdnNodeCredsFut(adnNodeOptFut, adnId, pwOpt) flatMap {
            // Это админ текущего узла
            case Right(adnNode) =>
              SioReqMd.fromPwOptAdn(pwOpt, adnId) flatMap { srm =>
                povAdnNodeOptFut flatMap { povAdnNodeOpt =>
                  val req1 = RequestForAdnNode(adnNode, povAdnNodeOpt, isMyNode = true, request, pwOpt, srm)
                  block(req1)
                }
              }

            // Узел существует, но он не относится к текущему залогиненному юзеру. Это узел третьего лица, рекламодателя в частности.
            case Left(Some(adnNode)) =>
              povAdnNodeOptFut flatMap { povAdnNodeOpt =>
                val srmFut = povAdnNodeOpt match {
                  // Это админ pov-узла подглядывает к рекламодателю или какому-то иному узлу.
                  case Some(povAdnNode) =>
                    SioReqMd.fromPwOptAdn(pwOpt, povAdnNode.id.get)
                  // Это кто-то иной косит под админа внешнего узела, скорее всего получил ссылку на ноду от другого человека.
                  case None =>
                    SioReqMd.fromPwOpt(pwOpt)
                }
                srmFut flatMap { srm =>
                  val req1 = RequestForAdnNode(adnNode, povAdnNodeOpt, isMyNode = false, request, pwOpt, srm)
                  block(req1)
                }
              }

            // Узел не существует.
            case Left(None) =>
              errorHandler.http404Fut(request)
          }

        // Отправить анонима на страницу логина.
        case None =>
          onUnauth(request)
      }
    }
  }

  /**
   * Доступ к узлу, к которому НЕ обязательно есть права на админство.
   * @param adnId узел.
   */
  case class AdnNodeAccessGet(adnId: String, povAdnIdOpt: Option[String])
    extends AdnNodeAccessBase
    with ExpireSession[RequestForAdnNode]
    with CsrfGet[RequestForAdnNode]

}


case class RequestForAdnNode[A](
  adnNode       : MNode,
  povAdnNodeOpt : Option[MNode],
  isMyNode      : Boolean,
  request       : Request[A],
  pwOpt         : PwOpt_t,
  sioReqMd      : SioReqMd
)
  extends AbstractRequestForAdnNode(request)
{

  def myNode: Option[MNode] = if (isMyNode) Some(adnNode) else povAdnNodeOpt
  def myNodeId: Option[String] = myNode.flatMap(_.id)
}

