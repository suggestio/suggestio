package models.usr

import java.util.UUID

import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.sec.QsbSigner
import io.suggest.sec.m.SecretKeyInit
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.19 19:57
  * Description: Подписанная qs-модель с данными для восстановления пароля через email.
  * Используется для ссылки в письме.
  */
object MEmailRecoverQs extends SecretKeyInit {

  private var SIGN_SECRET: String = _

  def getNowSec(): Long =
    System.currentTimeMillis() / 1000

  override def setSignSecret(secretKey: String): Unit = {
    SIGN_SECRET = secretKey
  }

  // Вешаем на общий ключ, т.к. идентификация переезжает на гос.услуги, и тут код - для совместимости.
  override def CONF_KEY = "play.http.secret.key"


  object Fields {
    def EMAIL_FN    = "e"
    def NOW_FN      = "t"
    def NONCE_FN    = "k"
    def NODE_ID_FN  = "n"
    def CHECK_SESSION = "s"
  }

  @inline implicit def univEq: UnivEq[MEmailRecoverQs] = UnivEq.derive

  implicit def emailRecoverQsQsb(implicit
                                 strB     : QueryStringBindable[String],
                                 strOptB  : QueryStringBindable[Option[String]],
                                 longB    : QueryStringBindable[Long],
                                 uuidB    : QueryStringBindable[UUID],
                                 boolB    : QueryStringBindable[Boolean],
                                ): QueryStringBindable[MEmailRecoverQs] = {
    new QueryStringBindableImpl[MEmailRecoverQs] {

      def getQsbSigner(key: String) = new QsbSigner(SIGN_SECRET, "sig")

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MEmailRecoverQs]] = {
        val k = key1F( key )
        for {
          params1       <- getQsbSigner (key).signedOrNone( k(""), params )
          emailE        <- strB.bind    (k(Fields.EMAIL_FN),      params1 )
          nodeIdOptE    <- strOptB.bind (k(Fields.NODE_ID_FN),    params1 )
          nowSecE       <- longB.bind   (k(Fields.NOW_FN),        params1 )
          nonceE        <- uuidB.bind   (k(Fields.NONCE_FN),      params1 )
          checkSessionE <- boolB.bind   (k(Fields.CHECK_SESSION), params1 )
        } yield {
          for {
            email       <- emailE
            nodeIdOpt   <- nodeIdOptE
            nowSec      <- nowSecE
            nonce       <- nonceE
            checkSession <- checkSessionE
          } yield {
            MEmailRecoverQs(
              email     = email,
              nodeId    = nodeIdOpt,
              nowSec    = nowSec,
              nonce     = nonce,
              checkSession = checkSession,
            )
          }
        }
      }

      override def unbind(key: String, value: MEmailRecoverQs): String = {
        val k = key1F( key )
        val unsigned = _mergeUnbinded1(
          strB.unbind     ( k(Fields.EMAIL_FN),   value.email   ),
          strOptB.unbind  ( k(Fields.NODE_ID_FN), value.nodeId  ),
          longB.unbind    ( k(Fields.NOW_FN),     value.nowSec  ),
          uuidB.unbind    ( k(Fields.NONCE_FN),   value.nonce   ),
          boolB.unbind    ( k(Fields.CHECK_SESSION), value.checkSession ),
        )
        getQsbSigner(key)
          .mkSigned(key, unsigned)
      }

    }
  }

}


/** Модель-контейнер данных для восстановления почты.
  * В отличии от древней модели EmailActivation, эта модель stateless с датой выдачи.
  *
  * @param email Почтовый адрес, указанный юзером в форме восстановления пароля.
  * @param nodeId Для использования в качестве инвайта.
  *               Для остальных случаев должен быть None.
  * @param nowSec Таймштамп, чтобы знать дату-время создания подписи.
  * @param nonce Рандомная строка-примесь для большей ротации подписи.
  * @param checkSession Надо ли проверять сессию на предмет наличия в ней nonce?
  *                     true - для обычно регистрации или восстановления пароля.
  *                     false - для всяких sys и прочих.
  */
case class MEmailRecoverQs(
                            email             : String,
                            nodeId            : Option[String]    = None,
                            nowSec            : Long              = MEmailRecoverQs.getNowSec(),
                            nonce             : UUID              = UUID.randomUUID(),
                            checkSession      : Boolean           = false,
                          )

