package util.acl

import com.google.inject.{Inject, Singleton}
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.www.util.acl.SioActionBuilderOuter
import models.req.{MAdProdNodesChainReq, MUserInit}
import play.api.mvc.{ActionBuilder, Request, Result, Results}
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.03.17 14:27
  * Description: ACL проверки возможности бесплатного управления прямым размещением карточки на каком-то либо узле.
  * Поддерживается задание id целевого узела задаётся по rcvrKey.
  */
@Singleton
class CanFreelyAdvAdOnNode @Inject() (
                                       aclUtil                  : AclUtil,
                                       canAdvAd                 : CanAdvAd,
                                       isNodeAdmin              : IsNodeAdmin,
                                       implicit private val ec  : ExecutionContext
                                     )
  extends SioActionBuilderOuter
  with MacroLogsImpl
{

  /** Выполнение проверки полного доступа к карточке и к цепочки узлов.
    *
    * @param adId id запрашиваемой рекламной карточки.
    * @param nodeKey Ключ узла в виде списка-цепочки узлов.
    * @param userInits1 Инициализация user-модели, если требуется.
    */
  def apply(adId: String, nodeKey: RcvrKey, userInits1: MUserInit*): ActionBuilder[MAdProdNodesChainReq] = {
    new SioActionBuilderImpl[MAdProdNodesChainReq] with InitUserCmds {

      override def userInits = userInits1

      override def invokeBlock[A](request: Request[A], block: (MAdProdNodesChainReq[A]) => Future[Result]): Future[Result] = {
        val ireq = aclUtil.reqFromRequest(request)

        val madReqOptFut = canAdvAd.maybeAllowed(adId, ireq)
        val user = ireq.user
        val nodesChainOptFut = isNodeAdmin.isNodeChainAdmin(nodeKey, user)

        maybeInitUser(user)

        def logPrefix = s"apply($adId, ${nodeKey.mkString("/")}):"

        madReqOptFut.flatMap {
          // Проверка доступа к карточке выполнена успешно.
          case Some(madProdReq) =>
            // Теперь проверить узлы, заданные в nodeKey:
            nodesChainOptFut.flatMap {
              // Успешно проверена вся цепочка узлов. Можно запускать код экшена.
              case Some(nodesChain) =>
                val mreq2 = MAdProdNodesChainReq(
                  mad         = madProdReq.mad,
                  producer    = madProdReq.producer,
                  nodesChain  = nodesChain,
                  request     = ireq,
                  user        = user
                )
                block(mreq2)

              // Был доступ к карточке, но нет доступа на указанный узел. В норме этой ситуации не должно возникать.
              case None =>
                LOGGER.warn(s"$logPrefix User#$user have adv access to ad#$adId, but nodes chain access validation failed.")
                Results.Forbidden(s"Failed in ${nodeKey.mkString("/")}")
            }

          // Проверка прав на карточку рекламную не пройдена.
          case None =>
            LOGGER.warn(s"$logPrefix User have no access for ad#$adId.")
            Results.Forbidden(s"No access for ad#$adId")
        }

      }
    }
  }

}