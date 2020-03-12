package util.acl

import akka.stream.Materializer
import io.suggest.es.model.{EsModel, MEsNestedSearch}
import io.suggest.i18n.MsgCodes
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.node.{MNodeTypes, MNodes}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.Inject
import models.req.{IReq, MNodeInviteReq}
import models.usr.MEmailRecoverQs
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}
import japgolly.univeq._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.09.15 11:35
 * Description: ActionBuilder'ы для доступа к инвайтам на управление узлом через email.
 * Аддон подмешивается к контроллерам, где необходима поддержка NodeEact.
 */
class CanUseNodeInvite @Inject()(
                                  esModel                      : EsModel,
                                  mNodes                       : MNodes,
                                  aclUtil                      : AclUtil,
                                  isAuth                       : IsAuth,
                                  reqUtil                      : ReqUtil,
                                  implicit private val ec      : ExecutionContext,
                                  implicit private val mat     : Materializer,
                                )
  extends MacroLogsImpl
{

  import esModel.api._


  /** Сборка ActionBuider'ов, проверяющих запросы активации инвайта на узел.
    *
    * @param nodeId id узла, на который клиент пытается получить привелегии.
    * @param eaId id в модели EmailActivation.
    * @param notFoundF Что делать при проблемах?
    *                  $1 - reason
    *                  $2 - sio-реквест.
    */
  def apply(qs: MEmailRecoverQs)(notFoundF: (String, IReq[_]) => Future[Result]): ActionBuilder[MNodeInviteReq, AnyContent] = {
    // Работает так:
    // - Узел + emailQs => проверка timestamp;
    // - index refresh();
    // - поиск текущих owner'ов узла, чтобы не было другого owner-юзера с указанным email.
    new reqUtil.SioActionBuilderImpl[MNodeInviteReq] {
      override def invokeBlock[A](request: Request[A], block: (MNodeInviteReq[A]) => Future[Result]): Future[Result] = {
        val baseReq = aclUtil.reqFromRequest( request )

        lazy val logPrefix = s"(${qs.email} n#${qs.nodeId.orNull}):"

        val diffSec = MEmailRecoverQs.getNowSec() - qs.nowSec
        if (diffSec < 0 || diffSec > 3.days.toSeconds || qs.nodeId.isEmpty) {
          // Кто-то пытается использовать данные для восстановления email не по адресу.
          LOGGER.warn(s"$logPrefix [SEC] Invalid signed qs=$qs. remote=${baseReq.remoteClientAddress} user#${baseReq.user.personIdOpt}")
          notFoundF("", baseReq)

        } else {
          // Надо поискать запрошенный узел:
          val resFut = for {
            // Для снижения риска параллельных регистраций, делаем предварительный рефреш индекса.
            _ <- mNodes.refreshIndex()
            nodeId = qs.nodeId.get

            // Кэшировать после refresh'а.
            mnodeOpt <- mNodes.getByIdAndCache( nodeId )
            if {
              val r = mnodeOpt.nonEmpty
              if (!r)
                LOGGER.warn(s"$logPrefix Node $nodeId does not exists. remote=${baseReq.remoteClientAddress}")
              r
            }
            mnode = mnodeOpt.get

            // Собрать id всех узлов-владельцев.
            ownerIds = mnode.edges
              .withPredicateIterIds( MPredicates.OwnedBy )
              .toSet

            // Узнать, есть ли среди владельцев, владелец с уже имеющимся email.
            isAlreadyOwnedByEmailUser <- mNodes
              .multiGetCacheSrc( ownerIds )
              .map { mnode =>
                val hasEmailIdent = mnode.edges
                  .withPredicateIterIds( MPredicates.Ident.Email )
                  .contains( qs.email )
                if (hasEmailIdent)
                  LOGGER.warn(s"$logPrefix Email ident already exists on owner-node#${mnode.idOrNull}")
                hasEmailIdent
              }
              .runReduce(_ && _)
              .recover { case _: NoSuchElementException => false }

            if {
              val res = !isAlreadyOwnedByEmailUser
              if (!res)
                LOGGER.warn(s"$logPrefix Invite is already used by user.")
              res
            }

            // Нужно поискать юзера, который вообще владеет подобным email.
            existsingUsersWithEmail <- mNodes.dynSearch {
              new MNodeSearch {
                override val outEdges: MEsNestedSearch[Criteria] = {
                  val cr = Criteria(
                    predicates = MPredicates.Ident.Email :: Nil,
                    nodeIds    = qs.email :: Nil,
                  )
                  MEsNestedSearch(
                    clauses = cr :: Nil,
                  )
                }
                override def limit = 2
              }
            }

            // Должно быть не более 1 юзера с таким ident'ом:
            if {
              val isOk = existsingUsersWithEmail.lengthCompare(1) <= 0
              if (!isOk)
                LOGGER.warn(s"$logPrefix Two or more users found for email ident: ${existsingUsersWithEmail.iterator.flatMap(_.id).mkString(", ")}")
              isOk
            }

            existingUserOpt = existsingUsersWithEmail.headOption

            if {
              val isUser = existingUserOpt.fold(true)(_.common.ntype ==* MNodeTypes.Person)
              if (!isUser)
                LOGGER.error(s"$logPrefix Node#${existingUserOpt.flatMap(_.id).orNull} with email ident exists, but not a user type. This should not happen.")
              isUser
            }

            req2 = MNodeInviteReq(
              mnode         = mnode,
              nodeOwnerIds  = ownerIds,
              emailOwner    = existingUserOpt,
              request       = baseReq,
              user          = baseReq.user,
            )
            result <- {
              LOGGER.trace(s"$logPrefix Ok. existingUserByEmail=${existingUserOpt.flatMap(_.id).orNull}")
              block( req2 )
            }

          } yield {
            result
          }

          resFut.recoverWith { case ex: Throwable =>
            ex match {
              case _: NoSuchElementException =>
                LOGGER.warn(s"$logPrefix One or more checks invalid")
              case _ =>
                LOGGER.error(s"$logPrefix Checks ", ex)
            }
            notFoundF( MsgCodes.`Error`, baseReq )
          }
        }
      }

    }
  }

}
