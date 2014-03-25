package models

import MPersonIdent.IdTypes.MPersonIdentType
import io.suggest.model._
import org.elasticsearch.common.xcontent.XContentBuilder
import EsModel._
import io.suggest.util.SioEsUtil._
import play.api.Play.current
import com.lambdaworks.crypto.SCryptUtil
import io.suggest.util.StringUtil
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.03.14 16:50
 * Description: ES-Модель для работы с идентификациями юзеров.
 * Нужно для возможности юзеров логинится по-разному: persona, просто имя-пароль и т.д.
 * В suggest.io исторически была только persona, которая жила прямо в MPerson.
 * Все PersonIdent имеют общий формат, однако хранятся в разных типах в рамках одного индекса.
 */
object MPersonIdent {

  /** Список емейлов админов suggest.io. */
  def SU_EMAILS: Seq[String] = {
    // id'шники суперюзеров sio можно указыват через конфиг, но мыльники должны быть в известных доменах.
    current.configuration.getStringSeq("sio.superuser.emails")
      .map {
        _.view.filter {
          email => email.endsWith("@cbca.ru") || email.endsWith("@shuma.ru")
        }
      }
      .getOrElse(Seq(
        "konstantin.nikiforov@cbca.ru",
        "ilya@shuma.ru",
        "sasha@cbca.ru",
        "maksim.sharipov@cbca.ru"
      ))
  }
  
  def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = false),
    FieldId(path = KEY_ESFN)  // Для надежной защиты от двойных добавлений.
  )
  def generateMappingProps: List[DocField] = List(
    FieldString(
      id = PERSON_ID_ESFN,
      index = FieldIndexingVariants.not_analyzed,
      include_in_all = false
    ),
    FieldString(
      id = KEY_ESFN,
      index = FieldIndexingVariants.not_analyzed,
      include_in_all = true
    ),
    FieldString(
      id = VALUE_ESFN,
      index = FieldIndexingVariants.no,
      include_in_all = false
    ),
    FieldBoolean(
      id = IS_VERIFIED_ESFN,
      index = FieldIndexingVariants.no,
      include_in_all = false
    )
  )

  def generateMapping(typ: String, withStaticFields: Seq[Field] = Nil): XContentBuilder = jsonGenerator { implicit b =>
    IndexMapping(
      typ = typ,
      staticFields = withStaticFields ++ generateMappingStaticFields,
      properties = generateMappingProps
    )
  }

  /** Собрать все мыльники указанного юзера во всех подмоделях.
    * @param personId id юзера
    * @return Список email'ов юзера в неопределённом порядке, возможно даже с дубликатами.
    */
  def findAllEmails(personId: String)(implicit ec: ExecutionContext, client: Client): Future[Seq[String]] = {
    val personIdQuery = QueryBuilders.termQuery(PERSON_ID_ESFN, personId)
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(MozillaPersonaIdent.ES_TYPE_NAME, EmailPwIdent.ES_TYPE_NAME)
      .setQuery(personIdQuery)
      // TODO ограничить возвращаемые поля только необходимыми
      .execute()
      .map { searchResp =>
        searchResp.getHits.getHits.map { hit =>
          hit.getType match {
            case MozillaPersonaIdent.ES_TYPE_NAME => MozillaPersonaIdent.deserializeOne(hit.getId, hit.getSource).email
            case EmailPwIdent.ES_TYPE_NAME        => EmailPwIdent.deserializeOne(hit.getId, hit.getSource).email
          }
        }
      }
  }

  /** Типы поддерживаемых алгоритмов идентификаций. В базу пока не сохраняются. */
  object IdTypes extends Enumeration {
    type MPersonIdentType = Value
    val MOZ_PERSONA, EMAIL_PW, EMAIL_ACT = Value
  }


  // Настройки генерации хешей. Используется scrypt. Это влияет только на новые создаваемые хеши, не ломая совместимость
  // с уже сохранёнными. Размер потребляемой памяти можно рассчитать Size = (128 * COMPLEXITY * RAM_BLOCKSIZE) bytes.
  // По дефолту жрём 16 метров с запретом параллелизации.
  /** Cложность хеша scrypt. */
  val SCRYPT_COMPLEXITY     = current.configuration.getInt("ident.pw.scrypt.complexity") getOrElse 16384
  /** Размер блока памяти. */
  val SCRYPT_RAM_BLOCKSIZE  = current.configuration.getInt("ident.pw.scrypt.ram.blocksize") getOrElse 8
  /** Параллелизация. Позволяет ускорить вычисление функции. */
  val SCRYPT_PARALLEL       = current.configuration.getInt("ident.pw.scrypt.parallel") getOrElse 1

  /** Генерировать новый хеш с указанными выше дефолтовыми параметрами.
    * @param password Пароль, который надо захешировать.
    * @return Текстовый хеш в стандартном формате $s0$params$salt$key.
    */
  def mkHash(password: String): String = {
    SCryptUtil.scrypt(password, SCRYPT_COMPLEXITY, SCRYPT_RAM_BLOCKSIZE, SCRYPT_PARALLEL)
  }

  /** Проверить хеш scrypt с помощью переданного пароля.
    * @param password Проверяемый пароль.
    * @param hash Уже готовый хеш.
    * @return true, если пароль ок. Иначе false.
    */
  def checkHash(password: String, hash: String): Boolean = {
    SCryptUtil.check(password, hash)
  }
}

import MPersonIdent._

trait MPersonIdent[E <: MPersonIdent[E]] extends EsModelT[E] {
  def personId: String
  def idType: MPersonIdentType
  def key: String
  def value: Option[String]
  def isVerified: Boolean

  /** Определяется реализацией: надо ли записывать в хранилище значение isVerified. */
  def writeVerifyInfo: Boolean

  def writeJsonFields(acc: XContentBuilder) {
    acc
      .field(PERSON_ID_ESFN, personId)
      .field(KEY_ESFN, key)
    if (value.isDefined)
      acc.field(VALUE_ESFN, value.get)
    if (writeVerifyInfo)
      acc.field(IS_VERIFIED_ESFN, isVerified)
  }
}


trait MPersonIdentSubmodelStatic {
  def generateMappingProps: List[DocField] = MPersonIdent.generateMappingProps
  def generateMappingStaticFields: List[Field] = MPersonIdent.generateMappingStaticFields
}

/** Идентификации от mozilla-persona. */
object MozillaPersonaIdent extends EsModelStaticT[MozillaPersonaIdent] with MPersonIdentSubmodelStatic {

  val ES_TYPE_NAME = "mpiMozPersona"

  def applyKeyValue(acc: MozillaPersonaIdent): PartialFunction[(String, AnyRef), Unit] = {
    case (KEY_ESFN, value)        => acc.email = stringParser(value)
    case (PERSON_ID_ESFN, value)  => acc.personId = stringParser(value)
  }

  protected def dummy(id: String) = MozillaPersonaIdent(email=null, personId=null)

}

trait MPIWithEmail {
  def email: String
}

case class MozillaPersonaIdent(
  var email     : String,
  var personId  : String
) extends MPersonIdent[MozillaPersonaIdent] with MPersonLinks with MPIWithEmail {
  /** Сгенерить id. Если допустить, что тут None, то _id будет из взят из поля key согласно маппингу. */
  def id: Option[String] = Some(email)
  def key = email
  def idType = IdTypes.MOZ_PERSONA
  def value = None
  def isVerified = true
  def writeVerifyInfo: Boolean = false
  def companion = MozillaPersonaIdent
}


/** Статическая под-модель для хранения юзеров, живущих вне mozilla persona. */
object EmailPwIdent extends EsModelStaticT[EmailPwIdent] with MPersonIdentSubmodelStatic {
  val ES_TYPE_NAME: String = "mpiEmailPw"

  def applyKeyValue(acc: EmailPwIdent): PartialFunction[(String, AnyRef), Unit] = {
    case (KEY_ESFN, value)          => acc.email = stringParser(value)
    case (VALUE_ESFN, value)        => acc.pwHash = stringParser(value)
    case (IS_VERIFIED_ESFN, value)  => acc.isVerified = booleanParser(value)
    case (PERSON_ID_ESFN, value)    => acc.personId = stringParser(value)
  }

  protected def dummy(id: String): EmailPwIdent = EmailPwIdent(email=id, personId=null, pwHash=null)

  def getByEmail(email: String)(implicit ec: ExecutionContext, client: Client) = {
    getById(email)
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
      pwHash = mkHash(password),
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
  var email     : String,
  var personId  : String,
  var pwHash    : String,
  var isVerified: Boolean = EmailPwIdent.IS_VERIFIED_DFLT
) extends MPersonIdent[EmailPwIdent] with MPersonLinks with MPIWithEmail {
  def id: Option[String] = Some(email)
  def idType: MPersonIdentType = IdTypes.EMAIL_PW
  def key: String = email
  def companion = EmailPwIdent
  def writeVerifyInfo: Boolean = true
  def value: Option[String] = Some(pwHash)
  def checkPassword(password: String) = checkHash(password, pwHash)
}


/** Статическая часть модели [[EmailActivation]].
  * Модель нужна для хранения ключей для проверки/активации почтовых ящиков. */
object EmailActivation extends EsModelStaticT[EmailActivation] with MPersonIdentSubmodelStatic {

  val ES_TYPE_NAME: String = "mpiEmailAct"

  /** Длина генерируемых ключей для активации. */
  val KEY_LEN = current.configuration.getInt("ident.email.act.key.len") getOrElse 16

  /** Период ttl, если иное не указано в документе. Записывается в маппинг. */
  val TTL_DFLT = current.configuration.getString("ident.email.act.ttl.period") getOrElse "2d"

  /** Сгенерить новый рандомный ключ активации.
    * @return Строка из символов [a-zA-Z0-9].
    */
  def randomActivationKey = StringUtil.randomId(len = KEY_LEN)

  protected def dummy(id: String) = EmailActivation(id = Option(id), email = null, key = null)

  /** Сборка static-полей маппинга. В этом маппинге должен быть ttl, чтобы старые записи автоматически выпиливались. */
  override def generateMappingStaticFields: List[Field] = {
    FieldTtl(enabled = true, default = TTL_DFLT) :: super.generateMappingStaticFields
  }

  def applyKeyValue(acc: EmailActivation): PartialFunction[(String, AnyRef), Unit] = {
    case (KEY_ESFN, value)          => acc.key = stringParser(value)
    case (PERSON_ID_ESFN, value)    => acc.email = stringParser(value)
  }

  /** Найти элементы по ключу. */
  def findByKey(key: String)(implicit ec: ExecutionContext, client: Client): Future[Seq[EmailActivation]] = {
    val keyQuery = QueryBuilders.termQuery(KEY_ESFN, key)
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(keyQuery)
      .execute()
      .map { searchResp2list }
  }
}

/**
 * Запись об активации почты.
 * @param email Почта.
 * @param key Ключ содержит какие-то данные, необходимые для активации. Например, id магазина.
 */
case class EmailActivation(
  var email     : String,
  var key       : String = EmailActivation.randomActivationKey,
  var id        : Option[String] = None
) extends MPersonIdent[EmailActivation] with MPersonLinks {
  def personId = email
  def companion = EmailActivation
  def writeVerifyInfo: Boolean = false
  def idType = IdTypes.EMAIL_ACT
  def isVerified = true
  def value: Option[String] = None
}

