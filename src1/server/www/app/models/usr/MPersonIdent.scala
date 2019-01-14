package models.usr

import io.suggest.util.JacksonParsing.FieldsJsonAcc
import io.suggest.es.model.EsModelUtil._
import javax.inject.{Inject, Singleton}
import io.suggest.es.model.{EsModelPlayJsonStaticT, EsModelStaticT, EsModelT}
import io.suggest.es.util.SioEsUtil._
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import org.elasticsearch.index.query.QueryBuilders
import play.api.libs.json.{JsBoolean, JsString}

import scala.concurrent.Future

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
  mExtIdents        : MExtIdents,
  mCommonDi         : ICommonDi
)
  extends MacroLogsImpl
{

  import LOGGER._
  import mCommonDi.{ec, esClient}

  def IDENT_MODELS = List[EsModelStaticIdentT](emailPwIdents, mExtIdents)
  def MODELS: List[EsModelStaticIdentT] = emailActivations :: IDENT_MODELS

  // TODO Нужно дедублицировать код между разными find*() методами.

  /**
   * Найти по email во всех моделях ident-моделях.
   * @param email Адрес электронной почты, который является _id в ident-моделях.
   * @return Список абстрактных результатов в неопределённом порядке.
   */
  def findIdentsByEmail(email: String): Future[List[MPersonIdent]] = {
    val identModels = IDENT_MODELS
    val identModelTypes = identModels.map(_.ES_TYPE_NAME)
    val iq = QueryBuilders.idsQuery(identModelTypes : _*).addIds(email)
    val indices = identModels.map(_.ES_INDEX_NAME).distinct
    esClient.prepareSearch(indices : _*)
      .setQuery(iq)
      .executeFut()
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
  def findEmails(personId: String): Future[Seq[String]] = {
    val personIdQuery = QueryBuilders.termQuery(PERSON_ID_ESFN, personId)
    val identModels = IDENT_MODELS
    val identTypes = identModels.map(_.ES_TYPE_NAME)
    val indices = identModels.map(_.ES_INDEX_NAME).distinct
    esClient.prepareSearch(indices : _*)
      .setTypes(identTypes : _*)
      .setQuery(personIdQuery)
      // TODO ограничить возвращаемые поля только необходимыми
      .executeFut()
      .map { searchResp =>
        searchResp.getHits.getHits.flatMap { hit =>
          hit.getType match {
            case emailPwIdents.ES_TYPE_NAME =>
              val email = emailPwIdents.deserializeOne2(hit).email
              email :: Nil
            case mExtIdents.ES_TYPE_NAME =>
              mExtIdents.deserializeOne2(hit).email
          }
        }
      }
  }


  /** Оптовая сборка всех почтовых адресов для произвольного множества юзеров.
    *
    * @param personIds id юзеров.
    * @return Фьючерс с результатами поиска, сгруппированными по person_id.
    */
  def findPersonsEmails(personIds: Seq[String]): Future[Map[String, Set[String]]] = {
    if (personIds.isEmpty) {
      Future.successful( Map.empty )

    } else {
      val personIdQuery = QueryBuilders.termsQuery(PERSON_ID_ESFN, personIds: _*)
      val identModels = IDENT_MODELS
      val identTypes = identModels.map(_.ES_TYPE_NAME)
      val indices = identModels.map(_.ES_INDEX_NAME).distinct
      esClient.prepareSearch(indices : _*)
        .setTypes(identTypes : _*)
        .setQuery(personIdQuery)
        .executeFut()
        .map { searchResp =>
          searchResp
            .getHits
            .getHits
            .iterator
            .flatMap { hit =>
              hit.getType match {
                case emailPwIdents.ES_TYPE_NAME =>
                  val epwIdent = emailPwIdents.deserializeOne2(hit)
                  val kv = epwIdent.personId -> epwIdent.email
                  kv :: Nil
                case mExtIdents.ES_TYPE_NAME =>
                  val extIdent = mExtIdents.deserializeOne2(hit)
                  for (email <- extIdent.email) yield {
                    extIdent.personId -> email
                  }
              }
            }
            .toStream
            .groupBy(_._1)
            .map { case (k, vs) =>
              k -> vs.iterator.map(_._2).toSet
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
      FieldKeyword(PERSON_ID_ESFN, index = true, include_in_all = false),
      FieldKeyword(KEY_ESFN, index = true, include_in_all = true),
      FieldText(VALUE_ESFN, index = false, include_in_all = false),
      FieldBoolean(IS_VERIFIED_ESFN, index = true, include_in_all = false)
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
      .executeFut()
      .map { searchResp2stream }
  }

  def countByPersonId(personId: String): Future[Long] = {
    countByQuery( personIdQuery(personId) )
  }

}

trait MPIWithEmail {
  def email: String
}
