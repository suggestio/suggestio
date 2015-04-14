package controllers

import com.google.inject.Inject
import io.suggest.ym.model.{MCompany, MAdnNode}
import models.AdnNodesSearchArgs
import models.usr.{MExtIdent, EmailPwIdent, EmailActivation}
import play.api.i18n.MessagesApi
import util.acl.{IsSuperuserPerson, IsSuperuser}
import views.html.ident.reg.email.emailRegMsgTpl
import views.html.ident.recover.emailPwRecoverTpl
import views.html.sys1.person._
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

  def index = IsSuperuser { implicit request =>
    Ok(indexTpl())
  }

  /** Отрендерить на экран email-сообщение регистрации юзера. */
  def showRegEmail = IsSuperuser { implicit request =>
    Ok(emailRegMsgTpl(dummyEa))
  }

  /** Отрендерить email-сообщение восстановления пароля. */
  def showRecoverEmail = IsSuperuser { implicit request =>
    Ok(emailPwRecoverTpl(dummyEa))
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

    // Запускаем поиск ident'ов
    val epwIdentsFut = EmailPwIdent.findByPersonId(personId)
    val eidIdentsFut = MExtIdent.findByPersonId(personId)

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

    // TODO рендерим ident'ы
    // TODO рендерим конечный шаблон.
    ???
  }

}
