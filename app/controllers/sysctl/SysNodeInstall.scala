package controllers.sysctl

import controllers.{SioController, routes}
import models.Context
import play.api.data.Form
import play.api.i18n.{Lang, Messages}
import play.api.mvc.Result
import util.PlayMacroLogsI
import util.acl.{AbstractRequestForAdnNode, IsSuperuserAdnNode}
import util.di.INodesUtil
import views.html.sys1.market.adn.install._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.04.15 17:25
 * Description: Аддон для поддержки ручной установки узлов и данных для них на существующие узлы.
 */
object SysNodeInstall {

  sealed case class FormData(count: Int, lang: Lang)

  def mkForm: Form[FormData] = {
    import play.api.data.Forms._
    import util.FormUtil.uiLangM
    Form(
      mapping(
        "count" -> number(0, max = 50),
        "lang"  -> uiLangM()
      )
      { FormData.apply }
      { FormData.unapply }
    )
  }

}


import controllers.sysctl.SysNodeInstall._


/** Аддон для контроллера, добавляющий экшены для сабжа. */
trait SysNodeInstall
  extends SioController
  with PlayMacroLogsI
  with INodesUtil
  with IsSuperuserAdnNode
{

  import mCommonDi._

  /** Вернуть страницу с формой установки дефолтовых карточек на узлы. */
  def installDfltMads(adnId: String) = IsSuperuserAdnNodeGet(adnId).async { implicit request =>
    implicit val ctx = implicitly[Context]
    val fd = FormData(
      count = nodesUtil.INIT_ADS_COUNT,
      lang  = ctx.messages.lang
    )
    val form = mkForm.fill(fd)
    _installRender(form, Ok)(ctx, request)
  }

  /** Общий код экшенов, связанный с рендером html-ответа. */
  private def _installRender(form: Form[FormData], rs: Status)
                            (implicit ctx: Context, request: AbstractRequestForAdnNode[_]): Future[Result] = {
    for {
      srcNodes <- mNodeCache.multiGet(nodesUtil.ADN_IDS_INIT_ADS_SOURCE)
    } yield {
      val allLangs = Lang.availables.sortBy(_.code)
      val html = installDfltMadsTpl(allLangs, request.adnNode, form, srcNodes)(ctx)
      rs(html)
    }
  }

  /** Сабмит формы установки дефолтовых карточек. */
  def installDfltMadsSubmit(adnId: String) = IsSuperuserAdnNodePost(adnId).async { implicit request =>
    lazy val logPrefix = s"installDfltMadsSubmit($adnId):"
    mkForm.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(logPrefix + "Failed to bind form:\n " + formatFormErrors(formWithErrors) )
        _installRender(formWithErrors, NotAcceptable)
      },
      {fd =>
        // TODO Надо как-то сделать, чтобы это дело скастовалось автоматом.
        val msgs = new Messages(fd.lang, messagesApi)
        nodesUtil.installDfltMads(adnId, count = fd.count)(msgs)
          .map { madIds =>
            val count = madIds.size
            LOGGER.trace(s"$logPrefix Cloned ok $count mads: [${madIds.mkString(", ")}]")
            Redirect(routes.SysMarket.showAdnNode(adnId))
              .flashing(FLASH.SUCCESS -> s"Клонировано $count дефолтовых карточек.")
          }
      }
    )
  }

}
