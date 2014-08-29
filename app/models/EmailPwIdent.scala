package models

import MPersonIdent.IdTypes.MPersonIdentType
import io.suggest.model._
import models.MPersonIdent.IdTypes
import EsModel._
import scala.collection.Map
import scala.concurrent.ExecutionContext
import org.elasticsearch.client.Client
import util.PlayMacroLogsImpl
import io.suggest.event.SioNotifierStaticClientI
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 14:52
 * Description: Модель хранения пар email-пароль, где пароль стойко захеширован.
 */

/** Статическая под-модель для хранения юзеров, живущих вне mozilla persona. */
object EmailPwIdent extends MPersonIdentSubmodelStatic with PlayMacroLogsImpl {

  override type T = EmailPwIdent

  override val ES_TYPE_NAME: String = "mpiEmailPw"

  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    EmailPwIdent(
      email       = stringParser(m get KEY_ESFN),
      pwHash      = stringParser(m get VALUE_ESFN),
      isVerified  = booleanParser(m get IS_VERIFIED_ESFN),
      personId    = stringParser(m get PERSON_ID_ESFN)
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

}


/**
 * Идентификация по email и паролю.
 * @param email Электропочта.
 * @param personId id юзера.
 * @param pwHash Хеш от пароля.
 * @param isVerified false по умолчанию, true если почта выверена.
 */
case class EmailPwIdent(
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
trait EmailPwIdentJmxMBean extends EsModelJMXMBeanCommon
class EmailPwIdentJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase
  with EmailPwIdentJmxMBean
{
  override def companion = EmailPwIdent
}


