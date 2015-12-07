package controllers

import com.google.inject.Inject
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.MNodeTypes
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import models.mproj.MCommonDi
import models.usr._
import models.{Context, MNode}
import org.elasticsearch.search.sort.SortOrder
import util.acl.{IsSuperuser, IsSuperuserPerson}
import views.html.ident.recover.emailPwRecoverTpl
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
class SysPerson @Inject() (
  override val mCommonDi    : MCommonDi
)
  extends SioControllerImpl
  with IsSuperuserPerson
  with IsSuperuser
{

  import mCommonDi._

  /** Генерация экземпляра EmailActivation с бессмысленными данными. */
  private def dummyEa = EmailActivation(
    email = "admin@suggest.io",
    key   = "keyKeyKeyKeyKey",
    id    = Some("IdIdIdIdId888")
  )

  def index = IsSuperuser.async { implicit request =>
    val personsCntFut: Future[Long] = {
      val psearch = new MNodeSearchDfltImpl {
        override def nodeTypes  = Seq(MNodeTypes.Person)
        override def limit      = Int.MaxValue    // TODO Надо ли оно тут вообще?
      }
      MNode.dynCount(psearch)
    }
    val epwIdsCntFut = EmailPwIdent.countAll
    val extIdsCntFut = MExtIdent.countAll
    val suCnt        = MPersonIdent.SU_EMAILS.size
    for {
      personsCnt <- personsCntFut
      epwIdsCnt  <- epwIdsCntFut
      extIdsCnt  <- extIdsCntFut
    } yield {
      Ok(indexTpl(
        personsCnt = personsCnt,
        epwIdsCnt  = epwIdsCnt,
        extIdsCnt  = extIdsCnt,
        suCnt      = suCnt
      ))
    }
  }

  /** Отрендерить на экран email-сообщение регистрации юзера. */
  def showRegEmail = IsSuperuser { implicit request =>
    Ok(emailRegMsgTpl(dummyEa))
  }

  /** Отрендерить email-сообщение восстановления пароля. */
  def showRecoverEmail = IsSuperuser { implicit request =>
    Ok(emailPwRecoverTpl(dummyEa))
  }

  /** Отрендерить страницу, которая будет содержать таблицу со всеми email+pw идентами. */
  def allEpws(offset: Int) = IsSuperuser.async { implicit request =>
    val limit = 20
    val epwsFut = EmailPwIdent.getAll(limit, offset = offset)
    for {
      epws <- epwsFut
    } yield {
      Ok(epwsListTpl(
        epws        = epws,
        limit       = limit,
        currOffset  = offset
      ))
    }
  }

  /** Отрендерить страницу с листингом внешних идентов. */
  def allExtIdents(offset: Int) = IsSuperuser.async { implicit request =>
    val limit = 20
    val extIdentsFut = MExtIdent.getAll(limit, offset = offset)
    for {
      extIdents <- extIdentsFut
    } yield {
      Ok(extIdentsListTpl(
        extIdents   = extIdents,
        limit       = limit,
        currOffset  = offset
      ))
    }
  }

  /**
   * Показать страницу с инфой по юзеру.
   * @param personId id просматриваемого юзера.
   * @return Страница с кучей ссылок на ресурсы, относящиеся к юзеру.
   */
  def showPerson(personId: String) = IsSuperuserPerson(personId).async { implicit request =>
    // Сразу запускаем поиск узлов: он самый тяжелый тут.
    val msearch = new MNodeSearchDfltImpl {
      override def outEdges  = {
        Seq(
          Criteria(Seq(personId), Seq(MPredicates.OwnedBy))
        )
      }
      override def withNameSort = Some(SortOrder.ASC)
    }
    val nodesFut = MNode.dynSearch( msearch )
    // Запускаем поиски ident'ов. Сортируем результаты.
    val epwIdentsFut = EmailPwIdent.findByPersonId(personId)
      .map { _.sortBy(_.email) }
    val extIdentsFut = MExtIdent.findByPersonId(personId)
      .map { _.sortBy { ei => ei.providerId + "." + ei.userId } }

    // Определить имя юзера, если возможно.
    val personNameOptFut = MPerson.findUsernameCached(personId)
    // Карта имен юзеров, передается в шаблоны.
    val personNamesFut = personNameOptFut
      .map { _.map(personId -> _).toMap }

    // Отображаемое на текущей странице имя юзера
    val personNameFut = personNameOptFut
      .map { _.getOrElse(personId) }

    implicit val ctx = implicitly[Context]

    // Рендер epw-идентов
    val epwIdentsHtmlFut = for {
      epws        <- epwIdentsFut
      personNames <- personNamesFut
    } yield {
      _epwIdentsTpl(epws, showPersonId = false, personNames = personNames)(ctx)
    }

    // Рендер ext-ident'ов
    val extIdentsHtmlFut = for {
      extIdents   <- extIdentsFut
      personNames <- personNamesFut
    } yield {
      _extIdentsTpl(extIdents, showPersonId = false, personNames = personNames)(ctx)
    }

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
      extIdentsHtml <- extIdentsHtmlFut
      epwIdentsHtml <- epwIdentsHtmlFut
      personName    <- personNameFut
    } yield {
      val contents = Seq(epwIdentsHtml, extIdentsHtml, nodesHtml)
      Ok( showPersonTpl(request.mperson, personName, contents)(ctx) )
    }
  }

}
