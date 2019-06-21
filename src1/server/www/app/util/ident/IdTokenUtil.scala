package util.ident

import io.suggest.id.token.{MIdMsg, MIdToken}
import io.suggest.sec.util.PgpUtil
import io.suggest.streams.JioStreamsUtil
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
                           ) { outer =>

  /** Проверить, соответствуют ли токену текущий реквест?
    * @return true, если всё нормально.
    */
  def isConstaintsMeetRequest(idToken: MIdToken)(implicit reqHdr: IReqHdr): Boolean = {
    val cs = idToken.constraints
    cs.personIdsC
      .fold(true) { personIdsOk =>
        val u = reqHdr.user.personIdOpt
        // Требуется анонимус или юзер из списка:
        (personIdsOk.isEmpty && u.isEmpty) || u.exists(personIdsOk.contains)
      }
  }



  /** Можно ли рестартовать указанную фазу проверки? */
  /*
  def restartMsg(rcpt: MIdMsgRcpt, data: Seq[MIdMsgData]): Either[String, ] = {
    rcpt.rcptType match {
      // Юзер хочет переслать смс-код повторно.
      case MPredicates.Ident.Phone =>
        ???
    }
  }
  */


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
