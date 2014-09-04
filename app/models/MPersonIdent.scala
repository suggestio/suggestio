package models

import MPersonIdent.IdTypes.MPersonIdentType
import io.suggest.model._
import org.elasticsearch.common.xcontent.XContentBuilder
import EsModel._
import io.suggest.util.SioEsUtil._
import play.api.Play.current
import com.lambdaworks.crypto.SCryptUtil
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import scala.collection.JavaConversions._
import play.api.libs.json.{JsBoolean, JsString}
import util.PlayMacroLogsImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.03.14 16:50
 * Description: ES-Модель для работы с идентификациями юзеров.
 * Нужно для возможности юзеров логинится по-разному: persona, просто имя-пароль и т.д.
 * В suggest.io исторически была только persona, которая жила прямо в MPerson.
 * Все PersonIdent имеют общий формат, однако хранятся в разных типах в рамках одного индекса.
 */
object MPersonIdent extends PlayMacroLogsImpl {

  import LOGGER._

  /** Список емейлов админов suggest.io. */
  def SU_EMAILS: Seq[String] = {
    // id'шники суперюзеров sio можно указыват через конфиг, но мыльники должны быть в известных доменах.
    current.configuration.getStringSeq("sio.superuser.emails")
      .map {
        _.view.filter { email =>
          val result = email.endsWith("@cbca.ru") || email.endsWith("@shuma.ru")
          if (!result)
            warn("SU_EMAILS(): Superuser email ignored: " + email + " : Invalid domain or other problem.")
          result
        }
      }
      .getOrElse(Seq(
        "konstantin.nikiforov@cbca.ru",
        "ilya@shuma.ru",
        "sasha@cbca.ru",
        "maksim.sharipov@cbca.ru",
        "alexander.pestrikov@cbca.ru"
      ))
  }

  // TODO Нужно дедублицировать код между разными find*() методами.

  /**
   * Найти по email во всех моделях ident-моделях.
   * @param email Адрес электронной почты, который является _id в ident-моделях.
   * @return Список абстрактных результатов в неопределённом порядке.
   */
  def findIdentsByEmail(email: String)(implicit ec: ExecutionContext, client: Client): Future[List[MPersonIdent]] = {
    val identModels = IdTypes.onlyIdents
    val identModelTypes = identModels.map(_.companion.ES_TYPE_NAME)
    val iq = QueryBuilders.idsQuery(identModelTypes : _*).addIds(email)
    val indices = identModels.map(_.companion.ES_INDEX_NAME).distinct
    client.prepareSearch(indices : _*)
      .setQuery(iq)
      .execute()
      .map { searchResp =>
        searchResp.getHits.getHits.foldLeft[List[MPersonIdent]] (Nil) { (acc, hit) =>
          // Выбрать десериализатор исходя из типа.
          val result1Opt = identModels
            .find(_.companion.ES_TYPE_NAME == hit.getType)
            .map {
             _.companion
              .deserializeOne(Option(hit.getId), hit.getSource, rawVersion2versionOpt(hit.getVersion))
            }
          result1Opt match {
            case Some(result1) =>
              result1 :: acc
            // should never occur
            case None =>
              error(s"findIdentsByEmail($email): Unexpected search hit received with type = [${hit.getType}]; possible types are ${identModels.mkString("[","], [","]")}\n $hit")
              acc
          }
        }
      }
  }
  
  def generateMappingStaticFields: List[Field] = {
    // Для надежной защиты от двойных добавлений.
    FieldId(path = KEY_ESFN) :: generateMappingStaticFieldsMin
  }

  def generateMappingStaticFieldsMin: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = false)
  )

  def generateMappingProps: List[DocField] = List(
    FieldString(PERSON_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldString(KEY_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
    FieldString(VALUE_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
    FieldBoolean(IS_VERIFIED_ESFN, index = FieldIndexingVariants.no, include_in_all = false)
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
    val identModels = IdTypes.onlyIdents
    val identTypes = identModels.map(_.companion.ES_TYPE_NAME)
    val indices = identModels.map(_.companion.ES_INDEX_NAME).distinct
    client.prepareSearch(indices : _*)
      .setTypes(identTypes : _*)
      .setQuery(personIdQuery)
      // TODO ограничить возвращаемые поля только необходимыми
      .execute()
      .map { searchResp =>
        searchResp.getHits.getHits.map { hit =>
          hit.getType match {
            case MozillaPersonaIdent.ES_TYPE_NAME =>
              MozillaPersonaIdent.deserializeOne(Option(hit.getId), hit.getSource, rawVersion2versionOpt(hit.getVersion)).email
            case EmailPwIdent.ES_TYPE_NAME =>
              EmailPwIdent.deserializeOne(Option(hit.getId), hit.getSource, rawVersion2versionOpt(hit.getVersion)).email
          }
        }
      }
  }

  /** Типы поддерживаемых алгоритмов идентификаций. В базу пока не сохраняются. */
  object IdTypes extends Enumeration {
    protected case class Val(companion: EsModelStaticIdentT, isIdent: Boolean) extends super.Val

    type MPersonIdentType = Val
    val MOZ_PERSONA = Val(MozillaPersonaIdent, isIdent = true)
    val EMAIL_PW    = Val(EmailPwIdent, isIdent = true)
    val EMAIL_ACT   = Val(EmailActivation, isIdent = true)

    implicit def value2val(x: Value): MPersonIdentType = x.asInstanceOf[MPersonIdentType]

    def onlyIdents = {
      values.foldLeft [List[Val]] (Nil) {
        (acc, e) =>
          if (e.isIdent)
            e :: acc
          else
            acc
      }
    }
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
    * @return Текстовый хеш в стандартном формате \$s0\$params\$salt\$key.
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

trait MPersonIdent extends EsModelPlayJsonT with EsModelT {
  override type T <: MPersonIdent

  def personId: String
  def idType: MPersonIdentType
  def key: String
  def value: Option[String]
  def isVerified: Boolean

  /** Определяется реализацией: надо ли записывать в хранилище значение isVerified. */
  def writeVerifyInfo: Boolean

  def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    var acc1: FieldsJsonAcc = PERSON_ID_ESFN -> JsString(personId) ::
      KEY_ESFN -> JsString(key) ::
      acc
    if (value.isDefined)
      acc1 ::= VALUE_ESFN -> JsString(value.get)
    if (writeVerifyInfo)
      acc1 ::= IS_VERIFIED_ESFN -> JsBoolean(isVerified)
    acc1
  }

}


trait EsModelStaticIdentT extends EsModelStaticT {
  override type T <: MPersonIdent
}
trait MPersonIdentSubmodelStatic extends EsModelStaticIdentT {
  def generateMappingProps: List[DocField] = MPersonIdent.generateMappingProps
  def generateMappingStaticFields: List[Field] = MPersonIdent.generateMappingStaticFields
  def getByEmail(email: String)(implicit ec: ExecutionContext, client: Client) = {
    getById(email)
  }

  /**
   * Найти иденты для указанного personId.
   * @param personId id юзера.
   * @return Список подходящих результатов в неопределённом порядке.
   */
  def findByPersonId(personId: String)(implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    val qb = QueryBuilders.termQuery(PERSON_ID_ESFN, personId)
    prepareSearch
      .setQuery(qb)
      .execute()
      .map { searchResp2list }
  }
}

trait MPIWithEmail {
  def email: String
}


