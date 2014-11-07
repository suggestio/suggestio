package controllers

import play.api.data._
import play.api.libs.json.JsValue
import play.api.mvc.WebSocket
import util.img.{ImgFormUtil, SysGalEditWsActor}
import util.{FormUtil, PlayMacroLogsImpl}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.acl.{IsSuperuserGallery, PersonWrapper, IsSuperuser}
import util.event.SiowebNotifier.Implicts.sn
import util.SiowebEsUtil.client
import models.im.{MImg, MGallery}
import play.api.Play.{current, configuration}
import play.api.data.Forms._
import views.html.sys1.galleries._
import util.img.ImgFormUtil.imgIdOptM

import scala.concurrent.Future

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


  /** form-маппер для имени (идентификатора) галереи. */
  private def galNameM: Mapping[String] = {
    nonEmptyText(maxLength = 128)
      .transform(FormUtil.strTrimSanitizeLowerF, FormUtil.strIdentityF)
      .verifying("error.required", !_.isEmpty)
      .verifying("error.invalid.gallery.name", { v => v match {
        case rawName =>
          "[-a-z0-9_]+".r.pattern.matcher(rawName).matches()
      }})
  }
  private def galNameKM = "name" -> galNameM

  private def galDescrM: Mapping[Option[String]] = {
    val m0 = text(maxLength = 1024)
    FormUtil.toStrOptM(m0, FormUtil.strTrimSanitizeF)
  }
  private def galDescrKM = "descr" -> galDescrM

  private def galImgsM: Mapping[List[MImg]] = {
    list(imgIdOptM)
      .transform[List[MImg]]( _.flatten, _.map(Some.apply) )
      .verifying("error.required", _.nonEmpty)
  }
  private def galImgsKM = "imgs" -> galImgsM

  /** Форма для создания галереи. */
  private def galCreateFormM: Form[MGallery] = {
    Form(
      mapping(galNameKM, galImgsKM, galDescrKM)
      {(name, imgs, descr) =>
        MGallery(name = name, imgs = imgs, descr = descr)
      }
      {mgal =>
        Some((mgal.name, mgal.imgs, mgal.descr))
      }
    )
  }

  /** Форма редактирования галереи. name менять нельзя, остальное тоже самое. */
  private def galEditFormM: Form[MGallery] = {
    Form(
      mapping(galImgsKM, galDescrKM)
      { (imgs, descr) => MGallery(name = null, imgs = imgs, descr = descr) }
      { mgal => Some((mgal.imgs, mgal.descr)) }
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

  /** Страница для отображения одной конкретной галереи. */
  def showOne(galId: String) = IsSuperuserGallery(galId) { implicit request =>
    Ok(showOneGalTpl(request.gallery))
  }

  /**
   * Веб-сокет для страниц с формами создания/редактирования галереи.
   * Изначально был создан для уведомлений о main-цвете загруженной картинки.
   * @param wsId id вебсокета.
   * @return Сокет для суперюзера.
   */
  def ws(wsId: String) = WebSocket.tryAcceptWithActor[JsValue, JsValue] { implicit request =>
    // Прямо тут проверяем права доступа для суперюзера.
    val auth = PersonWrapper.isSuperuser( PersonWrapper.getFromRequest )
    Future.successful(
      if (auth) {
        Right(SysGalEditWsActor.props(_, wsId))
      } else {
        val result = Forbidden("Unathorized")
        Left(result)
      }
    )
  }

  /** Экшен для загрузки tmp-картинки. */
  def uploadImg(wsId: String) = IsSuperuser.async(parse.multipartFormData) { implicit request =>
    _handleTempImg(runEarlyColorDetector = true, wsId = Some(wsId))
  }


  /** Страница с формой создания новой галереи. */
  def createGal = IsSuperuser { implicit request =>
    Ok(addGalTpl(galCreateFormM))
  }

  /** Сабмит формы создания новой галереи. */
  def createGalSubmit = IsSuperuser.async { implicit request =>
    galCreateFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"createGalSubmit(): Failed to bind form:\n" + formatFormErrors(formWithErrors))
        NotAcceptable(addGalTpl(formWithErrors))
      },
      {gal0 =>
        // Отправляем галерею картинок на сохранение:
        val galUpdateFut = ImgFormUtil.updateOrigImgFull(needImgs = gal0.imgs, oldImgs = Nil)
          .filter(_.nonEmpty)
        galUpdateFut flatMap { imgs2 =>
          // Заносим в галерею данные о текущем юзере
          val gal1 = gal0.copy(
            modifiedBy = request.pwOpt.map(_.personId),
            imgs = imgs2.toList
          )
          gal1.save.map { galId =>
            Redirect(routes.SysGallery.showOne(galId))
              .flashing("success" -> s"Создана новая галерея с ${imgs2.size} картинками: $galId")
          }
        }
      }
    )
  }


  /** Страница с формой редактирования существующей галереи. */
  def editGal(galId: String) = IsSuperuserGallery(galId) { implicit request =>
    val gf = galEditFormM.fill(request.gallery)
    Ok(editGalTpl(request.gallery, gf))
  }

  /** Сабмит формы редактирования существующей галереии. */
  def editGalSubmit(galId: String) = IsSuperuserGallery(galId).async { implicit request =>
    galEditFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"editGalSubmit($galId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        NotAcceptable(editGalTpl(request.gallery, formWithErrors))
      },
      {gal2 =>
        // В фоне запускаем сохранение картинок.
        val imgsUpdateFut = ImgFormUtil.updateOrigImgFull(needImgs = gal2.imgs, oldImgs = request.gallery.imgs)
        // без filter nonEmpty, т.к. в таком случае консистентность уже нарушена // TODO Нужно внедрить пошаговое транзакционное обновление картинок.
        imgsUpdateFut flatMap { imgs2 =>
          val gal3 = request.gallery.copy(
            imgs = imgs2.toList,
            descr = gal2.descr
          )
          gal3.save.map { _galId =>
            Redirect(routes.SysGallery.showOne(_galId))
              .flashing("success" -> s"Галерея '${gal3.name}' обновлена.")
          }
        }
      }
    )
  }


  def deleteGallerySubmit(galId: String) = IsSuperuserGallery(galId).async { implicit request =>
    request.gallery.delete map { isDeleted =>
      val flash = if (isDeleted)
        "success" -> "Галерея удалена."
      else
        "error" -> "Кажется, галерея не была найдена."
      Redirect(routes.SysGallery.showList())
        .flashing(flash)
    }
  }

}
