package controllers

import com.google.inject.Inject
import io.suggest.ym.model.{MCompany, MAdnNode}
import models.AdnNodesSearchArgs
import models.usr._
import play.api.i18n.MessagesApi
import util.acl.{IsSuperuserPerson, IsSuperuser}
import views.html.ident.reg.email.emailRegMsgTpl
import views.html.ident.recover.emailPwRecoverTpl
import views.html.sys1.person._
import views.html.sys1.person.parts._
import views.html.sys1.market.adn._adnNodesListTpl
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.02.15 18:13
 * Description: sys-контроллер для доступа к юзерам.
 */
class SysPerson @Inject() (
  override val messagesApi: MessagesApi
)
  extends SioControllerImpl
{

  /** Генерация экземпляра EmailActivation с бессмысленными данными. */
  private def dummyEa = EmailActivation(
    email = "admin@suggest.io",
    key   = "keyKeyKeyKeyKey",
    id    = Some("IdIdIdIdId888")
  )

  def index = IsSuperuser.async { implicit request =>
    val personsCntFut = MPerson.countAll
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
    val limit = 30
    val epwsFut = EmailPwIdent.getAll(limit, offset = offset, withVsn = false)
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

  /**
   * Показать страницу с инфой по юзеру.
   * @param personId id просматриваемого юзера.
   * @return Страница с кучей ссылок на ресурсы, относящиеся к юзеру.
   */
  def showPerson(personId: String) = IsSuperuserPerson(personId).async { implicit request =>
    // Сразу запускаем поиск узлов: он самый тяжелый тут.
    val nodesFut = MAdnNode.dynSearch(new AdnNodesSearchArgs {
      override def anyOfPersonIds = Seq(personId)
      override def withNameSort   = true
    })
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

    // Рендер epw-идентов
    val epwIdentsHtmlFut = for {
      epws        <- epwIdentsFut
      personNames <- personNamesFut
    } yield {
      _epwIdentsTpl(epws, showPersonId = false, personNames = personNames)
    }

    // Рендер ext-ident'ов
    val extIdentsHtmlFut = for {
      extIdents   <- extIdentsFut
      personNames <- personNamesFut
    } yield {
      _extIdentsTpl(extIdents, showPersonId = false, personNames = personNames)
    }

    // Рендерим список узлов:
    val companiesFut = nodesFut
      .map { _.flatMap(_.companyId).toSet }
      .flatMap { MCompany.multiGet(_) }
      .map { _.iterator.flatMap { v => v.id.map { id => id -> v } }.toMap }
    val nodesHtmlFut = for {
      mnodes    <- nodesFut
      companies <- companiesFut
    } yield {
      _adnNodesListTpl(mnodes, Some(companies), withDelims = false)
    }

    // Рендерим конечный шаблон.
    for {
      nodesHtml     <- nodesHtmlFut
      extIdentsHtml <- extIdentsHtmlFut
      epwIdentsHtml <- epwIdentsHtmlFut
      personName    <- personNameFut
    } yield {
      val contents = Seq(epwIdentsHtml, extIdentsHtml, nodesHtml)
      Ok(showPersonTpl(request.mperson, personName, contents))
    }
  }

}
