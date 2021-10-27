package util.ident

import javax.inject.Inject
import controllers.routes
import io.suggest.es.model.{EsModel, MEsNestedSearch}
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.node.{MNodeTypes, MNodes}
import io.suggest.n2.node.search.MNodeSearch
import models.usr.MSuperUsers
import play.api.mvc._
import japgolly.univeq._
import play.api.inject.Injector
import util.ident.store.ICredentialsStorage

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 15:45
 * Description: Утиль для ident-контроллера и других вещей, связанных с идентификацией пользователей.
 */
final class IdentUtil @Inject() (
                                  injector              : Injector,
                                ) {

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val mSuperUsers = injector.instanceOf[MSuperUsers]
  private lazy val credentialsStorage = injector.instanceOf[ICredentialsStorage]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  def getRdrNodeIds(personId: String): Future[Seq[String]] = {
    import esModel.api._
    mNodes.dynSearchIds(
      new MNodeSearch {
        override val outEdges: MEsNestedSearch[Criteria] = {
          val cr = Criteria(
            nodeIds     = personId :: Nil,
            predicates  = MPredicates.OwnedBy :: Nil
          )
          MEsNestedSearch.plain( cr )
        }
        override val nodeTypes = MNodeTypes.AdnNode :: Nil
        // Нам тут не надо выводить элементы, нужно лишь определять кол-во личных кабинетов и данные по ним.
        override def limit = 2
      }
    )
  }

  /** При логине юзера по email-pw мы определяем его присутствие в маркете, и редиректим в ЛК магазина или в ЛК ТЦ. */
  def getRdrCallFor(personId: String): Future[Option[Call]] = {
    for {
      mnodeIds <- getRdrNodeIds( personId )
    } yield {

      val rdrOrNull: Call = if (mnodeIds.isEmpty) {
        // У юзера нет рекламных узлов во владении. Некуда его редиректить, вероятно ошибся адресом.
        null
      } else if (mnodeIds.size ==* 1) {
        // У юзера есть один рекламный узел
        val nodeId = mnodeIds.head
        routes.LkAds.adsPage(nodeId :: Nil)
      } else {
        // У юзера есть несколько узлов во владении. Нужно предоставить ему выбор.
        routes.MarketLkAdn.lkList()
      }

      Option(rdrOrNull)
        // Если некуда отправлять, а юзер - админ, то отправить в /sys/.
        .orElse {
          if ( mSuperUsers.isSuperuserId(personId) ) {
            Some(routes.SysMarket.sysIndex())
          } else {
            None
          }
        }
    }
  }


  def redirectCallUserSomewhere(personId: String): Future[Call] = {
    getRdrCallFor(personId).flatMap {
      // Уже ясно куда редиректить юзера
      case Some(rdr) =>
        Future.successful(rdr)

      // У юзера нет узлов
      case None =>
        import esModel.api._

        // TODO Отправить на форму регистрации, если логин через внешнего id прова.
        for {
          n <- credentialsStorage.hasAnyExtServiceCreds( personId )
        } yield {
          if (n) {
            // Есть идентификации через соц.сети. Вероятно, юзер не закончил регистрацию.
            routes.Ident.idpConfirm()
          } else {
            // Нет узлов, залогинился через emailPW, отправить в lkList, там есть кнопка добавления узла.
            routes.MarketLkAdn.lkList()
          }
        }
    }
  }

  /** Сгенерить редирект куда-нибудь для указанного юзера. */
  def redirectUserSomewhere(personId: String): Future[Result] = {
    redirectCallUserSomewhere(personId)
      .map { Results.Redirect }
  }

}
