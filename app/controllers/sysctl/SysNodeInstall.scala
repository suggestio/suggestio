package controllers.sysctl

import controllers.{routes, SioController}
import models.{MAdnNodeCache, Context}
import play.api.data.Form
import play.api.i18n.Lang
import play.twirl.api.Html
import util.PlayMacroLogsI
import util.SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.acl.{AbstractRequestForAdnNode, IsSuperuserAdnNodePost, IsSuperuserAdnNodeGet}
import util.adn.NodesUtil
import play.api.Play.current
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


import SysNodeInstall._


/** Аддон для контроллера, добавляющий экшены для сабжа. */
trait SysNodeInstall extends SioController with PlayMacroLogsI {

  /** Вернуть страницу с формой установки дефолтовых карточек на узлы. */
  def installDfltMads(adnId: String) = IsSuperuserAdnNodeGet(adnId).async { implicit request =>
    implicit val ctx = implicitly[Context]
    val fd = FormData(
      count = NodesUtil.INIT_ADS_COUNT,
      lang  = ctx.lang
    )
    val form = mkForm.fill(fd)
    _installRender(form)(ctx, request)
      .map { Ok(_) }
  }

  /** Общий код экшенов, связанный с рендером html-ответа. */
  private def _installRender(form: Form[FormData])(implicit ctx: Context, request: AbstractRequestForAdnNode[_]): Future[Html] = {
    for {
      srcNodes <- MAdnNodeCache.multiGet(NodesUtil.ADN_IDS_INIT_ADS_SOURCE)
    } yield {
      val allLangs = Lang.availables.sortBy(_.code)
      installDfltMadsTpl(allLangs, request.adnNode, form, srcNodes)(ctx)
    }
  }

  /** Сабмит формы установки дефолтовых карточек. */
  def installDfltMadsSubmit(adnId: String) = IsSuperuserAdnNodePost(adnId).async { implicit request =>
    lazy val logPrefix = s"installDfltMadsSubmit($adnId):"
    mkForm.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(logPrefix + "Failed to bind form:\n " + formatFormErrors(formWithErrors) )
        _installRender(formWithErrors)
          .map { NotAcceptable(_) }
      },
      {fd =>
        NodesUtil.installDfltMads(adnId, count = fd.count)(fd.lang)
          .map { madIds =>
            val count = madIds.size
            LOGGER.trace(s"$logPrefix Cloned ok $count mads: [${madIds.mkString(", ")}]")
            Redirect(routes.SysMarket.showAdnNode(adnId))
              .flashing("success" -> s"Клонировано $count дефолтовых карточек.")
          }
      }
    )
  }

}
