package models.usr

import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.es._
import EsModelUtil._
import io.suggest.model._
import org.elasticsearch.client.Client
import util.PlayMacroLogsImpl
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.Map
import scala.concurrent.ExecutionContext
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 14:52
 * Description: Модель хранения пар email-пароль, где пароль стойко захеширован.
 */

/** Статическая под-модель для хранения юзеров, живущих вне mozilla persona. */
object EmailPwIdent extends MPersonIdentSubmodelStatic with PlayMacroLogsImpl with EsmV2Deserializer {

  override type T = EmailPwIdent

  override val ES_TYPE_NAME: String = "mpiEmailPw"

  @deprecated("Delete it, deserializeOne2() is ready here.", "2015.sep.08")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    EmailPwIdent(
      email       = stringParser( m(KEY_ESFN) ),
      pwHash      = stringParser( m(VALUE_ESFN) ),
      isVerified  = m.get(IS_VERIFIED_ESFN).fold(false)(booleanParser),
      personId    = stringParser( m(PERSON_ID_ESFN) )
    )
  }

  /** По дефолту email'ы считать проверенными или нет? */
  def IS_VERIFIED_DFLT = false

  /**
   * Собрать экземпляр [[EmailPwIdent]].
   * @param email Электропочта.
   * @param personId id юзера.
   * @param password Пароль как он есть.
   * @param isVerified Флаг проверенности пароля.
   * @return Экземпляр [[EmailPwIdent]] с захешированным паролем.
   */
  def applyWithPw(email: String, personId:String, password:String, isVerified:Boolean = IS_VERIFIED_DFLT): EmailPwIdent = {
    EmailPwIdent(
      email = email,
      personId = personId,
      pwHash = MPersonIdent.mkHash(password),
      isVerified = isVerified
    )
  }

  /** JSON-десериализатор модели. */
  private val _reads0 = (
    (__ \ KEY_ESFN).read[String] and
    (__ \ PERSON_ID_ESFN).read[String] and
    (__ \ VALUE_ESFN).read[String] and
    (__ \ IS_VERIFIED_ESFN).readNullable[Boolean]
      .map { _ getOrElse IS_VERIFIED_DFLT }
  )(apply _)

  override protected def esDocReads(meta: IEsDocMeta): Reads[EmailPwIdent] = {
    _reads0
  }

}


/**
 * Идентификация по email и паролю.
 * @param email Электропочта.
 * @param personId id юзера.
 * @param pwHash Хеш от пароля.
 * @param isVerified false по умолчанию, true если почта выверена.
 */
final case class EmailPwIdent(
  email     : String,
  personId  : String,
  pwHash    : String,
  isVerified: Boolean = EmailPwIdent.IS_VERIFIED_DFLT
) extends MPersonIdent with MPersonLinks with MPIWithEmail {
  override type T = EmailPwIdent

  override def id: Option[String] = Some(email)
  override def idType: MPersonIdentType = IdTypes.EMAIL_PW
  override def key: String = email
  override def companion = EmailPwIdent
  override def writeVerifyInfo = true
  override def value: Option[String] = Some(pwHash)
  override def versionOpt = None

  def checkPassword(password: String) = MPersonIdent.checkHash(password, pwHash)
}


// JMX
trait EmailPwIdentJmxMBean extends EsModelJMXMBeanI
final class EmailPwIdentJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase
  with EmailPwIdentJmxMBean
{
  override def companion = EmailPwIdent
}



/** Binding формы подтверждения регистрации по email возвращает эти данные. */
case class EmailPwConfirmInfo(
  adnName: String,
  password: String
)

