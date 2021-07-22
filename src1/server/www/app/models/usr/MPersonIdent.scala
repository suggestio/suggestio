package models.usr

import io.suggest.common.empty.OptionUtil
import javax.inject.{Inject, Singleton}
import io.suggest.es.model._
import io.suggest.ext.svc.MExtService
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.sec.util.ScryptUtil
import io.suggest.text.Validators

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
        val msearch = new MNodeSearch {
          override val nodeTypes =
            MNodeTypes.Person :: Nil
          override def limit = 1
          override val outEdges: MEsNestedSearch[Criteria] = {
            val cr = Criteria(
              predicates  = MPredicates.Ident.Id :: Nil,
              nodeIds     = remoteUserId :: Nil,
              extService  = Some(extService :: Nil),
            )
            MEsNestedSearch.plain( cr )
          }
        }
        mNodes.dynSearchOne( msearch )
      }

      /** Поиск юзеров с указанным мыльником, которые может логинится по паролю.
        *
        * @param emailOrPhone Выверенный нормализованный адрес электронной почты.
        * @return Фьючерс с узлами, подходящими под запрос.
        */
      def findUsersByEmailPhoneWithPw(emailOrPhone: String): Future[Seq[MNode]] = {
        mNodes.dynSearch {
          new MNodeSearch {
            // По идее, тут не более одного.
            override def limit = 2
            override val testNode = OptionUtil.SomeBool.someFalse
            override val isEnabled = OptionUtil.SomeBool.someTrue
            override val nodeTypes = MNodeTypes.Person :: Nil
            override val outEdges: MEsNestedSearch[Criteria] = {
              val must = IMust.MUST
              // Есть проверенный email:
              val emailCr = Criteria(
                predicates  =
                  MPredicates.Ident.Email ::
                  MPredicates.Ident.Phone ::
                  Nil,
                nodeIds     = (
                  Validators.normalizeEmail(emailOrPhone) ::
                  Validators.normalizePhoneNumber(emailOrPhone) ::
                  Nil
                )
                  .filter(_.nonEmpty)
                  .distinct,
                flag        = OptionUtil.SomeBool.someTrue,
                must        = must,
                nodeIdsMatchAll = false,
              )
              // И есть пароль
              val pwCr = Criteria(
                predicates  = MPredicates.Ident.Password :: Nil,
                must        = must,
              )
              MEsNestedSearch.plain( emailCr, pwCr )
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
      def onlyWithPassword(password: String): LazyList[MNode] = {
        (for {
          mnode <- nodesFound.iterator
          pwEdge <- mnode.edges.withPredicateIter( MPredicates.Ident.Password )
          savedPwHash <- pwEdge.info.textNi.iterator
          if scryptUtil.checkHash(password, savedPwHash)
        } yield {
          mnode
        })
          .to( LazyList )
      }

    }

  }
}
