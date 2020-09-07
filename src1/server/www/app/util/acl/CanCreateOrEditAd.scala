package util.acl

import javax.inject.Inject
import io.suggest.util.logs.MacroLogsImpl
import models.req._
import play.api.mvc._
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut
import io.suggest.req.ReqUtil
import japgolly.univeq._
import play.api.http.{HttpErrorHandler, Status}
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.10.17 22:30
  * Description: Класс для сборки ACL, пригодного для унифицированной операции между create и edit Ad.
  *
  * Т.е. если есть указанный id карточки, то будет проверка доступа на редактирование карточки.
  * Иначе, будет проверка доступа к узлу-продьюсеру.
  */
final class CanCreateOrEditAd @Inject() (
                                          injector    : Injector,
                                        )
  extends MacroLogsImpl
{

  private lazy val canEditAd = injector.instanceOf[CanEditAd]
  private lazy val isNodeAdmin = injector.instanceOf[IsNodeAdmin]
  private lazy val reqUtil = injector.instanceOf[ReqUtil]
  private lazy val httpErrorHandler = injector.instanceOf[HttpErrorHandler]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  def apply(adIdOpt: Option[String], producerIdOpt: Option[String], userInits1: MUserInit*): ActionBuilder[MAdOptProdReq, AnyContent] = {
    lazy val logPrefix = s"(${adIdOpt.orElse(producerIdOpt).orNull})#${System.currentTimeMillis()}:"

    // Нельзя, чтобы было два Some или оба None.
    if (adIdOpt.isEmpty ==* producerIdOpt.isEmpty) {
      // ActionBuilder, всегда возвращающий ошибку независимо от входных данных:
      new reqUtil.SioActionBuilderImpl[MAdOptProdReq] {
        override def invokeBlock[A](request: Request[A], block: (MAdOptProdReq[A]) => Future[Result]): Future[Result] = {
          val msg = "Exact one arg expected"
          LOGGER.warn(s"$logPrefix $msg adId=${adIdOpt.orNull}, nodeId=${producerIdOpt.orNull}")
          httpErrorHandler.onClientError( request, Status.PRECONDITION_FAILED, msg )
        }
      }

    } else {
      // Сейчас ровно один из двух аргументов - Some, а другой None. Отработать две ситуации:
      adIdOpt
        // Отработать Some(adId)
        .map [ActionBuilder[MAdOptProdReq, AnyContent]] { adId =>
          canEditAd(adId).andThen {
            LOGGER.trace(s"$logPrefix Using adEdit ACL for ad#$adId")
            new reqUtil.ActionTransformerImpl[MAdProdReq, MAdOptProdReq] {
              override protected def transform[A](request: MAdProdReq[A]): Future[MAdOptProdReq[A]] = {
                MAdOptProdReq( Some(request.mad), request )
              }
            }
          }
        }
        // Если adId=None, то отработать Some(producerId):
        .getOrElse {
          val producerId = producerIdOpt.get
          isNodeAdmin(producerId).andThen {
            LOGGER.trace(s"$logPrefix Using isNodeAdmin ACL for node#$producerId")
            new reqUtil.ActionTransformerImpl[MNodeReq, MAdOptProdReq] {
              override protected def transform[A](request: MNodeReq[A]): Future[MAdOptProdReq[A]] = {
                MAdOptProdReq(None, request)
              }
            }
          }
        }
    }
  }

}
