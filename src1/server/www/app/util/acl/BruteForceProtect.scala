package util.acl

import akka.actor.ActorSystem
import javax.inject.Inject
import io.suggest.util.logs.MacroLogsImplLazy
import models.req.{BfpArgs, IReq}
import play.api.mvc._
import io.suggest.req.ReqUtil
import io.suggest.text.StringUtil
import play.api.cache.AsyncCacheApi
import play.api.http.{HttpErrorHandler, Status}
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration._
import play.api.mvc.Result

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 15:53
 * Description: Защита от брутфорса оформлена тут. Её можно юзать как на уровне ActionBuilder'ов, так и controller'ов.
 *
 *
 * Функция для защиты от брутфорса. Повзоляет сделать асинхронную задержку выполнения экшена в контроллере.
 *  Настраивается путём перезаписи констант. Если LAG = 333 ms, и DIVISOR = 3, то скорость ответов будет такова:
 *  0*333 = 0 ms (3 раза), затем 1*333 = 333 ms (3 раза), затем 2*333 = 666 ms (3 раза), и т.д.
 */
final class BruteForceProtect @Inject() (
                                          injector                : Injector,
                                          aclUtil                 : AclUtil,
                                          cacheApi                : AsyncCacheApi,
                                          implicit private val ec : ExecutionContext,
                                        )
  extends MacroLogsImplLazy
{

  private lazy val actorSystem = injector.instanceOf[ActorSystem]
  private lazy val defaultActionBuilder = injector.instanceOf[DefaultActionBuilder]
  private lazy val reqUtil = injector.instanceOf[ReqUtil]
  private lazy val httpErrorHandler = injector.instanceOf[HttpErrorHandler]


  private def _apply[A](args: BfpArgs, request0: Request[A])(f: IReq[A] => Future[Result]): Future[Result] = {
    val mreq = aclUtil.reqFromRequest(request0)
    val remoteClientAddr = mreq.remoteClientAddress
    def logPrefix = s"bruteForceProtect($remoteClientAddr): ${mreq.method} ${StringUtil.strLimitLen(mreq.uri, 30)} ::"

    // Для противодействию брутфорсу добавляем асинхронную задержку выполнения проверки по методике https://stackoverflow.com/a/17284760
    val ck = args.cachePrefix + remoteClientAddr
    cacheApi
      .get[Int](ck)
      .map(_ getOrElse 0)
      .flatMap { prevTryCount =>

        if (prevTryCount > args.tryCountDeadline) {
          // Наступил предел толерантности к атаке.
          LOGGER.warn(s"$logPrefix Too many bruteforce retries. Dropping request...")
          httpErrorHandler.onClientError(request0, Status.TOO_MANY_REQUESTS, "Too many requests. Do not want.")

        } else {
          val lagMs = {
            val lagLevel = prevTryCount / args.tryCountDivisor
            lagLevel * lagLevel * args.lagMs
          }

          val resultFut: Future[Result] = if (lagMs <= 0) {
            f(mreq)

          } else {
            // Нужно решить, что делать с запросом.
            if (lagMs > args.attackLagMs) {
              // Кажется, идёт брутфорс-атака.
              LOGGER.warn(s"$logPrefix Attack is going on! Inserting fat lag $lagMs ms, prev.try count = $prevTryCount.")
            } else {
              // Есть подозрение на брутфорс.
              LOGGER.debug(s"$logPrefix Inserting lag $lagMs ms, try = $prevTryCount")
            }

            val lagPromise = Promise[Result]()
            actorSystem
              .scheduler
              .scheduleOnce(lagMs.milliseconds) {
                lagPromise.completeWith( f(mreq) )
              }
            lagPromise.future
          }

          // Закинуть в кеш инфу о попытке
          cacheApi.set(ck, prevTryCount + 1, args.cacheTtl)
          // Вернуть ответ, не дожидаясь кэша...
          resultFut
        }
      }
  }


  /** Собрать экшен под указанные параметры. */
  def apply[A](args: BfpArgs = BfpArgs.default)(action: Action[A]): Action[A] = {
    defaultActionBuilder.async(action.parser) { request0 =>
      _apply(args, request0)( action.apply )
    }
  }

  /** Завернуть в экшен с дефолтовыми параметрами. */
  def apply[A](action: Action[A]): Action[A] = {
    apply()(action)
  }


  /** Собрать action builder под указанные параметры. */
  def b(args: BfpArgs = BfpArgs.default): ActionBuilder[IReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[IReq] {
      override def invokeBlock[A](request: Request[A], block: (IReq[A]) => Future[Result]): Future[Result] = {
        _apply(args, request)(block)
      }
    }
  }

  /** Собрать action builder с дефолтовыми параметрами. */
  def b: ActionBuilder[IReq, AnyContent] = {
    b()
  }

}
