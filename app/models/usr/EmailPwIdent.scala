package models.usr

import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.es._
import EsModelUtil._
import com.google.inject.{Inject, Singleton}
import com.lambdaworks.crypto.SCryptUtil
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
@Singleton
class EmailPwIdents @Inject() ()
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
      email       = stringParser( m(KEY_ESFN) ),
      pwHash      = stringParser( m(VALUE_ESFN) ),
      isVerified  = m.get(IS_VERIFIED_ESFN)
        .fold(false)(booleanParser),
      personId    = stringParser( m(PERSON_ID_ESFN) )
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
        { _.getOrElse(EmailPwIdent.IS_VERIFIED_DFLT) },
        { b => if (b) Some(b) else None }
      )
  )(EmailPwIdent.apply, unlift(EmailPwIdent.unapply))



  /**
   * Собрать экземпляр [[EmailPwIdent]].
   *
   * @param email Электропочта.
   * @param personId id юзера.
   * @param password Пароль как он есть.
   * @param isVerified Флаг проверенности пароля.
   * @return Экземпляр [[EmailPwIdent]] с захешированным паролем.
   */
  def applyWithPw(email: String, personId:String, password:String, isVerified: Boolean = EmailPwIdent.IS_VERIFIED_DFLT): EmailPwIdent = {
    EmailPwIdent(
      email       = email,
      personId    = personId,
      pwHash      = mkHash(password),
      isVerified  = isVerified
    )
  }



  // TODO Вынести scrypt-хеш из моделей в отдельную утиль?

  // Настройки генерации хешей. Используется scrypt. Это влияет только на новые создаваемые хеши, не ломая совместимость
  // с уже сохранёнными. Размер потребляемой памяти можно рассчитать Size = (128 * COMPLEXITY * RAM_BLOCKSIZE) bytes.
  // По дефолту жрём 16 метров с запретом параллелизации.
  /** Cложность хеша scrypt. */
  def SCRYPT_COMPLEXITY     = 16384 //current.configuration.getInt("ident.pw.scrypt.complexity") getOrElse
  /** Размер блока памяти. */
  def SCRYPT_RAM_BLOCKSIZE  = 8 //current.configuration.getInt("ident.pw.scrypt.ram.blocksize") getOrElse 8
  /** Параллелизация. Позволяет ускорить вычисление функции. */
  def SCRYPT_PARALLEL       = 1 //current.configuration.getInt("ident.pw.scrypt.parallel") getOrElse 1

  /** Генерировать новый хеш с указанными выше дефолтовыми параметрами.
 *
    * @param password Пароль, который надо захешировать.
    * @return Текстовый хеш в стандартном формате \$s0\$params\$salt\$key.
    */
  def mkHash(password: String): String = {
    SCryptUtil.scrypt(password, SCRYPT_COMPLEXITY, SCRYPT_RAM_BLOCKSIZE, SCRYPT_PARALLEL)
  }

  /** Проверить хеш scrypt с помощью переданного пароля.
 *
    * @param password Проверяемый пароль.
    * @param hash Уже готовый хеш.
    * @return true, если пароль ок. Иначе false.
    */
  def checkHash(password: String, hash: String): Boolean = {
    SCryptUtil.check(password, hash)
  }

}

/** Интерфейс для поле с DI-инстансами [[EmailPwIdents]]. */
trait IEmailPwIdentsDi {
  def emailPwIdents: EmailPwIdents
}


object EmailPwIdent {
  /** По дефолту email'ы считать проверенными или нет? */
  def IS_VERIFIED_DFLT = false
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
  isVerified: Boolean = EmailPwIdent.IS_VERIFIED_DFLT
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
  implicit val ec         : ExecutionContext,
  implicit val client     : Client,
  implicit val sn         : SioNotifierStaticClientI
)
  extends EsModelJMXBase
    with EmailPwIdentsJmxMBean
{
  override type X = EmailPwIdent
}



/** Binding формы подтверждения регистрации по email возвращает эти данные. */
case class EmailPwConfirmInfo(
  adnName: String,
  password: String
)

