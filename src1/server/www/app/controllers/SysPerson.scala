package controllers

import io.suggest.es.model.{EsModel, MEsNestedSearch}

import javax.inject.Inject
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.node.{MNodeTypes, MNodes}
import io.suggest.n2.node.search.MNodeSearch
import models.mctx.Context
import models.usr._
import org.elasticsearch.search.sort.SortOrder
import util.acl.{IsSu, IsSuPerson, SioControllerApi}
import views.html.ident.reg.email.emailRegMsgTpl
import views.html.sys1.market.adn._adnNodesListTpl
import views.html.sys1.person._
import views.html.sys1.person.parts._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.02.15 18:13
 * Description: sys-контроллер для доступа к юзерам.
 */
// TODO Замержить куски контроллера в отображение узла N2. Сейчас этот контроллер рисует неактуальные данные.
final class SysPerson @Inject() (
                                  sioControllerApi          : SioControllerApi,
                                ) {

  import sioControllerApi._
  import mCommonDi.{ec, csrf}
  import mCommonDi.current.injector

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val mSuperUsers = injector.instanceOf[MSuperUsers]
  private lazy val isSu = injector.instanceOf[IsSu]
  private lazy val isSuPerson = injector.instanceOf[IsSuPerson]


  /** Генерация экземпляра EmailActivation с бессмысленными данными. */
  private def dummyEa = MEmailRecoverQs(
    email = "admin@suggest.io"
  )

  def index = csrf.AddToken {
    isSu().async { implicit request =>
      import esModel.api._

      val personsCntFut: Future[Long] = {
        val psearch = new MNodeSearch {
          override val nodeTypes  = MNodeTypes.Person :: Nil
          override def limit      = Int.MaxValue    // TODO Надо ли оно тут вообще?
        }
        mNodes.dynCount(psearch)
      }

      val identsCntFut = mNodes.dynCount(
        new MNodeSearch {
          override val outEdges: MEsNestedSearch[Criteria] = {
            val cr = Criteria(
              predicates = MPredicates.Ident :: Nil
            )
            MEsNestedSearch(
              clauses = cr :: Nil,
            )
          }
        }
      )

      val suCnt        = mSuperUsers.SU_EMAILS.size
      for {
        personsCnt <- personsCntFut
        identsCnt  <- identsCntFut
      } yield {
        Ok(indexTpl(
          personsCnt = personsCnt,
          identsCnt  = identsCnt,
          suCnt      = suCnt
        ))
      }
    }
  }


  /** Отрендерить на экран email-сообщение регистрации юзера. */
  def showRegEmail = csrf.AddToken {
    isSu() { implicit request =>
      Ok(emailRegMsgTpl(dummyEa))
    }
  }


  /** Отрендерить страницу с листингом внешних идентов. */
  def allIdents(theOffset: Int) = csrf.AddToken {
    isSu().async { implicit request =>
      import esModel.api._

      val theLimit = 5
      for {
        nodesFound <- mNodes.dynSearch(
          new MNodeSearch {
            override def limit = theLimit
            override def offset = theOffset
            override val outEdges: MEsNestedSearch[Criteria] = {
              val cr = Criteria(
                predicates = MPredicates.Ident :: Nil
              )
              MEsNestedSearch(
                clauses = cr :: Nil,
              )
            }
          }
        )
      } yield {
        val nodesIdented = for {
          mnode <- nodesFound.iterator
        } yield {
          val idents = mnode.edges
            .withPredicateIter( MPredicates.Ident )
            .toSeq
          mnode -> idents
        }
        Ok(IdentsListTpl(
          idents      = nodesIdented.toSeq,
          limit       = theLimit,
          currOffset  = theOffset
        ))
      }
    }
  }

  /**
   * Показать страницу с инфой по юзеру.
   *
   * @param personId id просматриваемого юзера.
   * @return Страница с кучей ссылок на ресурсы, относящиеся к юзеру.
   */
  def showPerson(personId: String) = csrf.AddToken {
    isSuPerson(personId).async { implicit request =>
      import esModel.api._

      // Сразу запускаем поиск узлов: он самый тяжелый тут.
      val nodesFut = mNodes.dynSearch(
        new MNodeSearch {
          override val outEdges: MEsNestedSearch[Criteria] = {
            val cr = Criteria(
              nodeIds    = personId :: Nil,
              predicates = MPredicates.OwnedBy :: Nil,
            )
            MEsNestedSearch(
              clauses = cr :: Nil,
            )
          }
          override val withNameSort = Some(SortOrder.ASC)
        }
      )

      // Отображаемое на текущей странице имя юзера
      val personName = request.mperson.guessDisplayNameOrIdOrQuestions

      val idents = request.mperson.edges
        .withPredicateIter( MPredicates.Ident )
        .toSeq

      implicit val ctx = implicitly[Context]

      // Рендер идентов:
      val identsHtml = _IdentsTpl( idents )(ctx)

      val nodesHtmlFut = for {
        mnodes    <- nodesFut
      } yield {
        _adnNodesListTpl(
          mnodes        = mnodes,
          withAdnDelims = false,
          withNtype     = true
        )(ctx)
      }

      // Рендерим конечный шаблон.
      for {
        nodesHtml     <- nodesHtmlFut
      } yield {
        val contents = identsHtml :: nodesHtml :: Nil
        Ok( showPersonTpl(request.mperson, personName, contents)(ctx) )
      }
    }
  }

}
