package util.ident

import javax.inject.{Inject, Singleton}
import controllers.routes
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.{MNodeTypes, MNodes}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import models.mproj.ICommonDi
import models.usr.{MExtIdents, MSuperUsers}
import play.api.mvc._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 15:45
 * Description: Утиль для ident-контроллера и других вещей, связанных с идентификацией пользователей.
 */
@Singleton
class IdentUtil @Inject() (
  mNodes        : MNodes,
  mSuperUsers   : MSuperUsers,
  mExtIdents    : MExtIdents,
  mCommonDi     : ICommonDi
) {

  import mCommonDi._

  /** При логине юзера по email-pw мы определяем его присутствие в маркете, и редиректим в ЛК магазина или в ЛК ТЦ. */
  def getMarketRdrCallFor(personId: String): Future[Option[Call]] = {

    // Нам тут не надо выводить элементы, нужно лишь определять кол-во личных кабинетов и данные по ним.
    val msearch = new MNodeSearchDfltImpl {
      override def outEdges: Seq[Criteria] = {
        val cr = Criteria(Seq(personId), Seq(MPredicates.OwnedBy))
        Seq(cr)
      }
      override def nodeTypes = Seq( MNodeTypes.AdnNode )
      override def limit = 2
    }

    for {
      mnodeIds <- mNodes.dynSearchIds(msearch)

    } yield {

      val rdrOrNull: Call = if (mnodeIds.isEmpty) {
        // У юзера нет рекламных узлов во владении. Некуда его редиректить, вероятно ошибся адресом.
        null
      } else if (mnodeIds.size == 1) {
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
    getMarketRdrCallFor(personId).flatMap {
      // Уже ясно куда редиректить юзера
      case Some(rdr) =>
        Future.successful(rdr)

      // У юзера нет узлов
      case None =>
        // TODO Отправить на форму регистрации, если логин через внешнего id прова.
        for {
          n <- mExtIdents.countByPersonId(personId)
        } yield {
          if (n > 0L) {
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

  // TODO Нужен метод "инсталляции" нового юзера.
}
