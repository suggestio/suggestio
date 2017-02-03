package models.usr

import io.suggest.model.es._
import EsModelUtil._
import com.google.inject.{Inject, Singleton}
import io.suggest.util.JacksonParsing
import models.mproj.ICommonDi
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
@Singleton
class EmailPwIdents @Inject() (
  override val mCommonDi: ICommonDi
)
  extends MPersonIdentSubmodelStatic
    with PlayMacroLogsImpl
    with EsmV2Deserializer
    with EsModelJsonWrites
{

  override type T = EmailPwIdent

  override val ES_TYPE_NAME: String = "mpiEmailPw"

  @deprecated("Delete it, deserializeOne2() is ready here.", "2015.sep.08")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    EmailPwIdent(
      email       = JacksonParsing.stringParser( m(KEY_ESFN) ),
      pwHash      = JacksonParsing.stringParser( m(VALUE_ESFN) ),
      isVerified  = m.get(IS_VERIFIED_ESFN)
        .fold(false)( JacksonParsing.booleanParser ),
      personId    = JacksonParsing.stringParser( m(PERSON_ID_ESFN) )
    )
  }


  override protected def esDocReads(meta: IEsDocMeta): Reads[EmailPwIdent] = {
    FORMAT
  }
  override def esDocWrites: Writes[EmailPwIdent] = FORMAT


  /** JSON-десериализатор модели. */
  implicit val FORMAT = (
    (__ \ KEY_ESFN).format[String] and
    (__ \ PERSON_ID_ESFN).format[String] and
    (__ \ VALUE_ESFN).format[String] and
    (__ \ IS_VERIFIED_ESFN).formatNullable[Boolean]
      .inmap [Boolean] (
        { _.getOrElse(false) },
        { b => if (b) Some(b) else None }
      )
  )(EmailPwIdent.apply, unlift(EmailPwIdent.unapply))

}


/** Интерфейс для поле с DI-инстансами [[EmailPwIdents]]. */
trait IEmailPwIdentsDi {
  def emailPwIdents: EmailPwIdents
}


/**
 * Идентификация по email и паролю.
 *
 * @param email Электропочта.
 * @param personId id юзера.
 * @param pwHash Хеш от пароля.
 * @param isVerified false по умолчанию, true если почта выверена.
 */
final case class EmailPwIdent(
  email     : String,
  personId  : String,
  pwHash    : String,
  isVerified: Boolean
)
  extends MPersonIdent
    with MPIWithEmail
{
  override def id: Option[String] = Some(email)
  override def idType: MPersonIdentType = IdTypes.EMAIL_PW
  override def key: String = email
  override def writeVerifyInfo = true
  override def value: Option[String] = Some(pwHash)
  override def versionOpt = None
}


// JMX
trait EmailPwIdentsJmxMBean extends EsModelJMXMBeanI
final class EmailPwIdentsJmx @Inject()(
  override val companion  : EmailPwIdents,
  override val ec         : ExecutionContext
)
  extends EsModelJMXBaseImpl
    with EmailPwIdentsJmxMBean
{
  override type X = EmailPwIdent
}



/** Binding формы подтверждения регистрации по email возвращает эти данные. */
case class EmailPwConfirmInfo(
  adnName: String,
  password: String
)

