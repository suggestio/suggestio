package models.usr

import io.suggest.util.JacksonParsing.FieldsJsonAcc
import io.suggest.es.model.EsModelUtil._
import javax.inject.{Inject, Singleton}
import io.suggest.es.model._
import io.suggest.es.util.SioEsUtil._
import io.suggest.ext.svc.MExtService
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.sec.util.ScryptUtil
import play.api.libs.json.{JsBoolean, JsString}

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


trait MPersonIdentSubmodelStatic extends EsModelStaticIdentT

@Singleton
class MPersonIdentModel @Inject()(
                                   esModel    : EsModel,
                                   scryptUtil : ScryptUtil,
                                 )(implicit ec: ExecutionContext) {
  import esModel.api._

  object api {

    implicit class N2NodesIdentsOps( mNodes: MNodes ) {

      /** Поддержка ident'ов поверх MNodes.
        *
        * @param extService Сервис, на котором проходит идентификация.
        * @param remoteUserId id юзера на стороне сервиса.
        * @return Фьючерс с опционально-найденым юзером-узлом.
        */
      def getByUserIdProv(extService: MExtService, remoteUserId: String): Future[Option[MNode]] = {
        val msearch = new MNodeSearchDfltImpl {
          override def nodeTypes =
            MNodeTypes.Person :: Nil
          override def limit = 1
          override def outEdges: Seq[Criteria] = {
            val cr = Criteria(
              predicates  = MPredicates.Ident.Id :: Nil,
              nodeIds     = remoteUserId :: Nil,
              extService  = Some(extService :: Nil),
            )
            cr :: Nil
          }
        }
        mNodes.dynSearchOne( msearch )
      }

      /** Поиск юзеров с указанным мыльником, которые может логинится по паролю.
        *
        * @param email Выверенный нормализованный адрес электронной почты.
        * @return Фьючерс с узлами, подходящими под запрос.
        */
      def findUsersByEmailWithPw(email: String): Future[Stream[MNode]] = {
        mNodes.dynSearch {
          new MNodeSearchDfltImpl {
            // По идее, тут не более одного.
            override def limit = 10
            override val nodeTypes = MNodeTypes.Person :: Nil
            override val outEdges: Seq[Criteria] = {
              val must = IMust.MUST
              // Есть проверенный email:
              val emailCr = Criteria(
                predicates  = MPredicates.Ident.Email :: Nil,
                nodeIds     = email :: Nil,
                flag        = Some(true),
                must        = must
              )
              // И есть пароль
              val pwCr = Criteria(
                predicates  = MPredicates.Ident.Password :: Nil,
                must        = must,
              )
              emailCr :: pwCr :: Nil
            }
          }
        }
      }

    }


    /** Доп.API для работы с идентами в списке найденных узлов-юзеров. */
    implicit class N2NodesFoundOpsExt( nodesFound: Iterable[MNode] ) {

      /** Найти узлы, для которых подходит указанный пароль.
        *
        * @param password Пароль.
        * @return Узел.
        */
      def onlyWithPassword(password: String): Stream[MNode] = {
        (for {
          mnode <- nodesFound.iterator
          pwEdge <- mnode.edges.withPredicateIter( MPredicates.Ident.Password )
          savedPwHash <- pwEdge.info.textNi.iterator
          if scryptUtil.checkHash(password, savedPwHash)
        } yield {
          mnode
        })
          .toStream
      }

    }

  }
}
