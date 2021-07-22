package util.acl

import io.suggest.es.model.{EsDocVersion, EsModel, MEsNestedSearch}
import io.suggest.n2.edge.{MEdge, MNodeEdges, MPredicates}
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.node.common.MNodeCommon
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}

import javax.inject.Inject
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.req._
import models.usr._
import play.api.mvc._
import japgolly.univeq._
import play.api.http.HttpVerbs
import play.api.inject.Injector

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 16:08
 * Description:
 * ActionBuilder для некоторых экшенов восстановления пароля. Завязан на некоторые фунции контроллера, поэтому
 * лежит здесь.
 *
 * Всё сделано в виде аддона для контроллера, т.к. DI-зависимость так проще всего разрулить.
 */

final class CanRecoverPw @Inject() (
                                     injector: Injector,
                                   )
  extends MacroLogsImpl
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val aclUtil = injector.instanceOf[AclUtil]
  private lazy val reqUtil = injector.instanceOf[ReqUtil]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]


  /** Собрать ACL ActionBuilder проверки доступа на восстановление пароля.
    *
    * @param keyNotFoundF Не найден ключ для восстановления.
    */
  def apply(qs: MEmailRecoverQs, userInits1: MUserInit*)
           (keyNotFoundF: IReq[_] => Future[Result]): ActionBuilder[MNodeReq, AnyContent] = {

    new reqUtil.SioActionBuilderImpl[MNodeReq] {

      override def invokeBlock[A](request: Request[A], block: (MNodeReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)
        lazy val logPrefix = s"${qs.email}#${user.personIdOpt.orNull}:"

        def runF(mnode: MNode): Future[Result] = {
          val req1 = MNodeReq(mnode, request, user)
          block(req1)
        }

        def _reqErr = MReq(request, user)

        val nowSec = MEmailRecoverQs.getNowSec()
        val qsAgeSec = nowSec - qs.nowSec

        if (qsAgeSec > 0L && qsAgeSec <= 6.hours.toSeconds && qs.nodeId.isEmpty) {
          import esModel.api._

          val searchPersonsFut = mNodes.dynSearch {
            new MNodeSearch {
              override val nodeTypes = MNodeTypes.Person :: Nil
              override val outEdges: MEsNestedSearch[Criteria] = {
                val cr = Criteria(
                  predicates  = MPredicates.Ident.Email :: Nil,
                  nodeIds     = qs.email :: Nil,
                )
                MEsNestedSearch.plain( cr )
              }
              // Макс. 2 юзера, чтобы определить неопределённую ситуацию.
              override def limit = 2
            }
          }

          MUserInits.initUser( user, userInits1 )

          searchPersonsFut.flatMap { mNodesFound =>
            if (mNodesFound.isEmpty) {
              // Нет найденных юзеров, почему-то.
              if (request.method ==* HttpVerbs.GET && user.isSuper) {
                // Это суперюзер рендерит шаблон письма. Mock'нуть узел:
                LOGGER.info(s"Mocked node for action ${request.uri}")
                val mockedMnode = MNode(
                  common = MNodeCommon(
                    ntype = MNodeTypes.Person,
                    isDependent = false
                  ),
                  edges = MNodeEdges(
                    out = MNodeEdges.edgesToMap(
                      MEdge(
                        predicate = MPredicates.Ident.Email,
                        nodeIds   = Set.empty + qs.email,
                      )
                    )
                  ),
                  // Защита от возможного логически-ошибочного сохранения mocked-узла в СУБД:
                  versioning = EsDocVersion.notSaveable,
                  id = Some("")
                )
                runF( mockedMnode )

              } else {
                LOGGER.warn(s"$logPrefix No users found for email=${qs.email}, but password recovery via email requested.")
                keyNotFoundF( _reqErr )
              }

            } else if (mNodesFound.lengthCompare(1) > 0) {
              // Почему-то найдено сразу два (или более) юзера. Это ненормальная ситуация, её быть не должно.
              throw new IllegalStateException(s"$logPrefix Too many users found: ${mNodesFound.iterator.flatMap(_.id).mkString(", ")}")

            } else {
              // Есть юзер, подходящий под описанный запрос.
              val personNode = mNodesFound.head
              LOGGER.trace(s"$logPrefix Password recovery action for user#${personNode.idOrNull}")
              runF(personNode)
            }
          }

        } else {
          val req1 = _reqErr
          LOGGER.warn(s"$logPrefix [SEC] Recover max-age invalid: diffSec=$qsAgeSec, user#${user.personIdOpt.orNull} remote=${req1.remoteClientAddress}")
          keyNotFoundF( req1 )
        }
      }

    }
  }

}
