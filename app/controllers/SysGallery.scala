package controllers

import play.api.data._
import util.{FormUtil, PlayMacroLogsImpl}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.acl.IsSuperuser
import util.event.SiowebNotifier.Implicts.sn
import util.SiowebEsUtil.client
import models.im.{MImg, MGallery}
import play.api.Play.{current, configuration}
import play.api.data.Forms._
import views.html.sys1.galleries._
import util.img.ImgFormUtil.imgIdOptM

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.11.14 18:48
 * Description: Контроллер для управления галереями картинок: создание, изменение, удаление.
 */
object SysGallery extends SioController with PlayMacroLogsImpl with TempImgSupport {

  import LOGGER._

  /** Макс.выдача на страницу. */
  private val MAX_PER_PAGE = configuration.getInt("sys.gallery.list.max.per.page") getOrElse 10


  /** form-маппер для имени галереи. */
  private def galNameM: Mapping[String] = {
    nonEmptyText(maxLength = 128)
      .transform(FormUtil.strTrimSanitizeLowerF, FormUtil.strIdentityF)
      .verifying("error.required", !_.isEmpty)
      .verifying("error.invalid.gallery.name", { v => v match {
        case rawName =>
          "[-a-z0-9_]+".r.pattern.matcher(rawName).matches()
      }})
  }

  /** Форма для создания/редактирования галереи. */
  private def galFormM: Form[MGallery] = {
    Form(
      mapping(
        "name" -> galNameM,
        "imgs" -> list(imgIdOptM)
          .transform[List[MImg]]( _.flatten, _.map(Some.apply) ),
        "descr" -> {
          val m0 = text(maxLength = 1024)
          FormUtil.toStrOptM(m0, FormUtil.strTrimSanitizeF)
        }
      )
      {(name, imgs, descr) =>
        MGallery(name = name, imgs = imgs, descr = descr)
      }
      {mgal =>
        Some((mgal.name, mgal.imgs, mgal.descr))
      }
    )
  }

  /**
   * Экшен отображения страницы со списком доступных галерей.
   * @param page номер текущей страницы.
   * @return 200 OK со страницей-таблицей.
   */
  def showList(page: Int) = IsSuperuser.async { implicit request =>
    val galsFut = MGallery.getAll(maxResults = MAX_PER_PAGE, offset = page * MAX_PER_PAGE, withVsn = false)
    galsFut map { gals =>
      Ok(galsListTpl(gals, page = page, pageSz = MAX_PER_PAGE))
    }
  }

  /**
   * Веб-сокет для страниц создания/редактирования галереи.
   * @param wsId id вебсокета.
   * @return Сокет для суперюзера.
   */
  def ws(wsId: String) = IsSuperuser.async { implicit request =>
    // TODO Нужно открывать ws наподобии того, который в MarketAd.
    ???
  }

  /** Экшен для загрузки tmp-картинки. */
  def uploadImg(wsId: String) = IsSuperuser.async(parse.multipartFormData) { implicit request =>
    _handleTempImg(runEarlyColorDetector = true, wsId = Some(wsId))
  }

  /** Страница с формой создания новой галереи. */
  def createGal = IsSuperuser.async { implicit request =>
    ???
  }

}
