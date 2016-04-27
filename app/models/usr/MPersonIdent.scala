package models.usr

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.model.es.{EsModelPlayJsonStaticT, EsModelStaticT, EsModelT, EsModelUtil}
import EsModelUtil._
import com.google.inject.{Inject, Singleton}
import io.suggest.util.SioEsUtil._
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import play.api.libs.json.{JsBoolean, JsString}
import util.PlayMacroLogsImpl

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.03.14 16:50
 * Description: ES-Модель для работы с идентификациями юзеров.
 * Нужно для возможности юзеров логинится по-разному: persona, просто имя-пароль и т.д.
 * В suggest.io исторически была только persona, которая жила прямо в MPerson.
 * Все PersonIdent имеют общий формат, однако хранятся в разных типах в рамках одного индекса.
 */
@Singleton
class MPersonIdents @Inject() (
  emailPwIdents     : EmailPwIdents,
  emailActivations  : EmailActivations,
  mExtIdents        : MExtIdents
)
  extends PlayMacroLogsImpl
{

  import LOGGER._

  def IDENT_MODELS = List[EsModelStaticIdentT](emailPwIdents, mExtIdents)
  def MODELS: List[EsModelStaticIdentT] = emailActivations :: IDENT_MODELS

  // TODO Нужно дедублицировать код между разными find*() методами.

  /**
   * Найти по email во всех моделях ident-моделях.
   * @param email Адрес электронной почты, который является _id в ident-моделях.
   * @return Список абстрактных результатов в неопределённом порядке.
   */
  def findIdentsByEmail(email: String)(implicit ec: ExecutionContext, client: Client): Future[List[MPersonIdent]] = {
    val identModels = IDENT_MODELS
    val identModelTypes = identModels.map(_.ES_TYPE_NAME)
    val iq = QueryBuilders.idsQuery(identModelTypes : _*).addIds(email)
    val indices = identModels.map(_.ES_INDEX_NAME).distinct
    client.prepareSearch(indices : _*)
      .setQuery(iq)
      .execute()
      .map { searchResp =>
        searchResp.getHits.getHits.foldLeft[List[MPersonIdent]] (Nil) { (acc, hit) =>
          // Выбрать десериализатор исходя из типа.
          val result1Opt = identModels
            .find(_.ES_TYPE_NAME == hit.getType)
            .map {
             _.deserializeOne2(hit)
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
  


  /** Собрать все мыльники указанного юзера во всех подмоделях.
    * @param personId id юзера
    * @return Список email'ов юзера в неопределённом порядке, возможно даже с дубликатами.
    */
  def findAllEmails(personId: String)(implicit ec: ExecutionContext, client: Client): Future[Seq[String]] = {
    val personIdQuery = QueryBuilders.termQuery(PERSON_ID_ESFN, personId)
    val identModels = IDENT_MODELS
    val identTypes = identModels.map(_.ES_TYPE_NAME)
    val indices = identModels.map(_.ES_INDEX_NAME).distinct
    client.prepareSearch(indices : _*)
      .setTypes(identTypes : _*)
      .setQuery(personIdQuery)
      // TODO ограничить возвращаемые поля только необходимыми
      .execute()
      .map { searchResp =>
        searchResp.getHits.getHits.flatMap { hit =>
          hit.getType match {
            case emailPwIdents.ES_TYPE_NAME =>
              val email = emailPwIdents.deserializeOne2(hit).email
              Seq(email)
            case mExtIdents.ES_TYPE_NAME =>
              mExtIdents.deserializeOne2(hit).email
          }
        }
      }
  }

}
/** Интерфейс для поля с DI-инстансом над-модели [[MPersonIdents]]. */
trait IMPersonIdents {
  def mPersonIdents: MPersonIdents
}


/** Трейт, который реализуют все экземпляры идентов. */
trait MPersonIdent extends EsModelT {

  /** id юзера в системе. */
  def personId: String

  /** Подтип PersonIdent. */
  def idType: MPersonIdentType

  /** Некий индексируемый ключ, по которому должен идти поиск/фильтрация.
    * В случае email-pw -- это юзернейм, т.е. email.
    * В случае соц.сетей -- это внетренний userId соц.сети. */
  def key: String

  /** Какое-то дополнительное НЕиндексируемое значение. В случае username+pw тут хеш пароля. */
  def value: Option[String]

  /** Проверенный ident? */
  def isVerified: Boolean

  /** Определяется реализацией: надо ли записывать в хранилище значение isVerified. */
  def writeVerifyInfo: Boolean

}


trait EsModelStaticIdentT extends EsModelStaticT with EsModelPlayJsonStaticT {
  override type T <: MPersonIdent

  /** Сериализация json-экземпляра. */
  override def writeJsonFields(m: T, acc: FieldsJsonAcc): FieldsJsonAcc = {
    import m._
    var acc1: FieldsJsonAcc = PERSON_ID_ESFN -> JsString(personId) ::
      KEY_ESFN -> JsString(key) ::
      acc
    if (value.isDefined)
      acc1 ::= VALUE_ESFN -> JsString(value.get)
    if (writeVerifyInfo)
      acc1 ::= IS_VERIFIED_ESFN -> JsBoolean(isVerified)
    acc1
  }


  def generateMappingStaticFields: List[Field] = {
    List(
      FieldSource(enabled = true),
      FieldAll(enabled = false)
    )
  }


  def generateMappingProps: List[DocField] = {
    List(
      FieldString(PERSON_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(KEY_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
      FieldString(VALUE_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldBoolean(IS_VERIFIED_ESFN, index = FieldIndexingVariants.no, include_in_all = false)
    )
  }

}
trait MPersonIdentSubmodelStatic extends EsModelStaticIdentT  {

  import mCommonDi._

  def personIdQuery(personId: String) = QueryBuilders.termQuery(PERSON_ID_ESFN, personId)

  def getByEmail(email: String) = {
    getById(email)
  }

  /**
   * Найти иденты для указанного personId.
   * @param personId id юзера.
   * @return Список подходящих результатов в неопределённом порядке.
   */
  def findByPersonId(personId: String): Future[Seq[T]] = {
    prepareSearch()
      .setQuery( personIdQuery(personId) )
      .execute()
      .map { searchResp2list }
  }

  def countByPersonId(personId: String): Future[Long] = {
    prepareCount()
      .setQuery( personIdQuery(personId) )
      .execute()
      .map { _.getCount }
  }

}

trait MPIWithEmail {
  def email: String
}


/** Типы поддерживаемых алгоритмов идентификаций. В базу пока не сохраняются. */
object IdTypes extends Enumeration with EnumMaybeWithName {

  /** Абстрактный экземпляр модели. */
  protected[this] class Val extends super.Val {
  }

  override type T = Val

  val EMAIL_PW: T = new Val

  val EMAIL_ACT: T = new Val

  val EXT_ID: T = new Val

}


