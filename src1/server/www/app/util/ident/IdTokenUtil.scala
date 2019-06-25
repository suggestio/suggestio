package util.ident

import java.time.{Instant, ZoneOffset}

import io.suggest.id.token.{MIdMsg, MIdToken}
import io.suggest.sec.util.PgpUtil
import io.suggest.streams.JioStreamsUtil
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import japgolly.univeq._
import models.req.IReqHdr

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.06.19 15:48
  * Description: Утиль для обработки id-токенов.
  */
@Singleton
class IdTokenUtil @Inject()(
                             pgpUtil                      : PgpUtil,
                             implicit private val ec      : ExecutionContext,
                           )
  extends MacroLogsImpl
{ outer =>

  /** Допустимое кол-во секунд для неточного сравнения с now. */
  private def NOW_LAG_SECONDS = 1

  /** Проверить, соответствуют ли токену текущий реквест?
    * @return true, если всё нормально.
    */
  def isConstaintsMeetRequest(idToken: MIdToken, now: Instant = Instant.now())(implicit reqHdr: IReqHdr): Boolean = {
    lazy val logPrefix = s"isConstaintsMeetRequest(${idToken.ottId}):"

    // Проверка даты создания токена и best-before.
    val isBestBeforeOk = {
      val bestBefore = idToken.dates.bestBefore
      val isBestBeforeOk0 = !(bestBefore isBefore now)
      if (!isBestBeforeOk0) LOGGER.warn(s"$logPrefix Token out of date. now=${now.atOffset(ZoneOffset.UTC)}, token bestBefore=${bestBefore.atOffset(ZoneOffset.UTC)}")
      isBestBeforeOk0
    }

    isBestBeforeOk && {
      // Убедится, что токен не из будущего. На случай, если на сервере время слетело.
      val dt = idToken.dates.modifiedOrCreated
      val now1 = (now plusSeconds NOW_LAG_SECONDS)
      val tokenInFuture = dt isAfter now1
      if (tokenInFuture) LOGGER.error(s"$logPrefix Token from future. now=${now1.atOffset(ZoneOffset.UTC)} (+${NOW_LAG_SECONDS}sec included) ;; token=${dt.atOffset(ZoneOffset.UTC)}")
      !tokenInFuture
    } && {
      // Проверить ограничение по personId в сессии:
      LOGGER.trace(s"$logPrefix Constraints: ${idToken.constraints}")
      idToken.constraints.personIdsC
        .fold {
          LOGGER.trace(s"$logPrefix No personId constaints. Skipping.")
          true
        } { personIdsOk =>
          val u = reqHdr.user.personIdOpt

          // Требуется анонимус или юзер из списка:
          val r = (personIdsOk.isEmpty && u.isEmpty) ||
            u.exists(personIdsOk.contains)
          if (!r) LOGGER.warn(s"$logPrefix IdToken Constaint mismatched: Req.person.id=$u, but expected ${personIdsOk.mkString(", ")}")
          r
        }
    }
  }


  /** Декодирование токена, принятого с клиента. */
  def decrypt( cipherText: String ): Future[MIdToken] = {
    val pgpKeyFut = pgpUtil.getLocalStorKey()
    val pgpMessage = pgpUtil.unminifyPgpMessage( cipherText )
    for {
      pk <- pgpKeyFut
    } yield {
      val jsonBytes = JioStreamsUtil.stringIo[Array[Byte]](pgpMessage, 512)( pgpUtil.decryptFromSelf(_, pk, _) )
      Json.parse( jsonBytes ).as[MIdToken]
    }
  }


  /** Кодирование токена перед отправкой на клиент. */
  def encrypt(idToken: MIdToken): Future[String] = {
    val pgpKeyFut = pgpUtil.getLocalStorKey()
    val jsonStr = Json
      .toJson( idToken )
      .toString()
    for {
      pk <- pgpKeyFut
    } yield {
      val pgpMessage = JioStreamsUtil.stringIo[String](jsonStr, 1024)( pgpUtil.encryptForSelf(_, pk, _) )
      pgpUtil.minifyPgpMessage( pgpMessage )
    }
  }

}


object IdTokenUtil {
  sealed trait IStep
  case class SendMsg(msg: MIdMsg) extends IStep
}
