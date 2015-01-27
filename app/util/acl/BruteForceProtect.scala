package util.acl

import com.typesafe.scalalogging.slf4j.Logger
import controllers.{SioController, MyConfName}
import play.api.cache.Cache
import play.api.mvc._
import util._
import scala.concurrent.{Promise, Future}
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._
import play.api.Play.{current, configuration}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Result


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 15:53
 * Description: Защита от брутфорса оформлена тут. Её можно юзать как на уровне ActionBuilder'ов, так и controller'ов.
 */

trait BruteForceProtectBase extends PlayMacroLogsI with MyConfName {

  /** Шаг задержки. Добавляемая задержка ответа будет кратна этому лагу. */
  val BRUTEFORCE_LAG_MS = configuration.getInt(s"bfp.$MY_CONF_NAME.lag_ms") getOrElse BRUTEFORCE_ATTACK_LAG_MS_DFLT
  def BRUTEFORCE_LAG_MS_DFLT = 222

  /** Префикс в кеше для ip-адреса. */
  val BRUTEFORCE_CACHE_PREFIX = configuration.getInt(s"bfp.$MY_CONF_NAME.cache.prefix") getOrElse BRUTEFORCE_CACHE_PREFIX_DFLT
  def BRUTEFORCE_CACHE_PREFIX_DFLT = "bfp:"

  /** Какой лаг уже считается лагом текущей атаки? (в миллисекундах) */
  val BRUTEFORCE_ATTACK_LAG_MS = configuration.getInt(s"bfp.$MY_CONF_NAME.attack.lag.ms") getOrElse BRUTEFORCE_ATTACK_LAG_MS_DFLT
  def BRUTEFORCE_ATTACK_LAG_MS_DFLT = 2000

  /** Нормализация кол-ва попыток происходит по этому целому числу. */
  val BRUTEFORCE_TRY_COUNT_DIVISOR = configuration.getInt(s"bfp.$MY_CONF_NAME.try.count.divisor") getOrElse BRUTEFORCE_TRY_COUNT_DEADLINE_DFLT
  def BRUTEFORCE_TRY_COUNT_DIVISOR_DFLT = 2

  /** Время хранения в кеше инфы о попытках для ip-адреса. */
  val BRUTEFORCE_CACHE_TTL = configuration.getInt(s"bfp.$MY_CONF_NAME.cache.ttl").getOrElse(BRUTEFORCE_CACHE_TTL_SECONDS_DFLT).seconds
  def BRUTEFORCE_CACHE_TTL_SECONDS_DFLT = 30

  /** Макс кол-во попыток, после которого запросы будут отправляться в помойку. */
  val BRUTEFORCE_TRY_COUNT_DEADLINE = configuration.getInt(s"bfp.$MY_CONF_NAME.cache.ttl") getOrElse BRUTEFORCE_TRY_COUNT_DEADLINE_DFLT
  def BRUTEFORCE_TRY_COUNT_DEADLINE_DFLT = 40

  def bruteForceLogPrefix(implicit request: RequestHeader): String = {
    s"bruteForceProtect($MY_CONF_NAME/${request.remoteAddress}): ${request.method} ${request.path}?${request.rawQueryString} : "
  }

  /** Система асинхронного платформонезависимого противодействия брутфорс-атакам. */
  def bruteForceProtected(f: => Future[Result])(implicit request: SioRequestHeader): Future[Result] = {
    // Для противодействию брутфорсу добавляем асинхронную задержку выполнения проверки по методике https://stackoverflow.com/a/17284760
    val ck = BRUTEFORCE_CACHE_PREFIX + request.remoteAddress
    val prevTryCount: Int = Cache.getAs[Int](ck) getOrElse 0
    if (prevTryCount > BRUTEFORCE_TRY_COUNT_DEADLINE) {
      // Наступил предел толерантности к атаке.
      onBruteForceDeadline(f, prevTryCount)
    } else {
      val lagMs = getLagMs(prevTryCount)
      val resultFut: Future[Result] = if (lagMs <= 0) {
        f
      } else {
        // Нужно решить, что делать с запросом.
        if (lagMs > BRUTEFORCE_ATTACK_LAG_MS) {
          // Кажется, идёт брутфорс-атака.
          onBruteForceAttackDetected(f, lagMs, prevTryCount = prevTryCount)
        } else {
          // Есть подозрение на брутфорс.
          onBruteForceAttackSuspicion(f, lagMs, prevTryCount = prevTryCount)
        }
      }
      // Закинуть в кеш инфу о попытке
      Cache.set(ck, prevTryCount + 1, BRUTEFORCE_CACHE_TTL)
      resultFut
    }
  }

  /** Формула рассчёта лага брутфорса. */
  def getLagMs(prevTryCount: Int): Int = {
    val lagLevel = prevTryCount / BRUTEFORCE_TRY_COUNT_DIVISOR
    lagLevel * lagLevel * BRUTEFORCE_LAG_MS
  }

  def onBruteForceReplyLagged(f: => Future[Result], lagMs: Int): Future[Result] = {
    val lagPromise = Promise[Result]()
    Akka.system.scheduler.scheduleOnce(lagMs milliseconds) {
      lagPromise completeWith f
    }
    lagPromise.future
  }

  /** Подозрение на брутфорс. В нормале - увеличивается лаг. */
  def onBruteForceAttackSuspicion(f: => Future[Result], lagMs: Int, prevTryCount: Int)(implicit request: SioRequestHeader): Future[Result] = {
    LOGGER.debug(s"${bruteForceLogPrefix}Inserting lag $lagMs ms, try = $prevTryCount")
    onBruteForceReplyLagged(f, lagMs)
  }

  /** Замечен брутфорс. */
  def onBruteForceAttackDetected(f: => Future[Result], lagMs: Int, prevTryCount: Int)(implicit request: SioRequestHeader): Future[Result] = {
    LOGGER.warn(s"${bruteForceLogPrefix}Attack is going on! Inserting fat lag $lagMs ms, prev.try count = $prevTryCount.")
    onBruteForceReplyLagged(f, lagMs)
  }

  /** Наступил дедлайн, т.е. атака точно подтверждена, и пора дропать запросы. */
  def onBruteForceDeadline(f: => Future[Result], prevTryCount: Int)(implicit request: SioRequestHeader): Future[Result] = {
    LOGGER.warn(bruteForceLogPrefix + "Too many bruteforce retries. Dropping request...")
    bruteForceRequestDrop
  }

  /** Если нет возможности использовать implicit request, тут явная версия: */
  def bruteForceProtectedNoimpl(request: SioRequestHeader)(f: => Future[Result]): Future[Result] = {
    bruteForceProtected(f)(request)
  }

  def bruteForceRequestDrop: Future[Result]

}

/** Для использования на уровне контроллера, можно юзать этот трейт. */
trait BruteForceProtectCtl extends BruteForceProtectBase with SioController {
  override def bruteForceRequestDrop: Future[Result] = {
    TooManyRequest("Too many requests. Do not want.")
  }
}

/** С минимумом зависимостей. */
trait BruteForceProtectSimple extends BruteForceProtectBase {
  override def bruteForceRequestDrop: Future[Result] = {
    Future successful Results.TooManyRequest("Too many requests. Do not want.")
  }
}

