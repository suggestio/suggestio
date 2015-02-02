package controllers

import java.net.{MalformedURLException, URL}

import models.im.MImg
import util.{FormUtil, PlayMacroLogsImpl}
import util.acl.IsSuperuser
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.event.SiowebNotifier.Implicts.sn
import util.SiowebEsUtil.client
import views.html.sys1.img._
import play.api.data._, Forms._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.14 11:40
 * Description: sys-раздел для работы с конкретными картинками.
 * Изначально была потребность быстро получать оригиналы картинок и получать прочую информацию по хранимым
 * изображениям.
 */
object SysImg extends SioControllerImpl with PlayMacroLogsImpl {

  import LOGGER._

  /** Маппинг для поисковой формы, который пытается распарсить ссылку с img qs или просто img qs. */
  def imgFormM: Form[MImg] = Form(
    "qstr" -> text(maxLength = 512)
      .transform [String] (FormUtil.strTrimSanitizeUnescapeF, FormUtil.strIdentityF)
      .verifying("error.required", !_.isEmpty)
      .transform[String] (
        {s =>
          try {
            val url = new URL(s)
            val q = url.getQuery
            if (q == null) s else q
          } catch {
            case ex: MalformedURLException => s
          }
        },
        { identity }
      )
      .transform [Option[MImg]] (
        {qs =>
          try {
            Some( MImg(qs) )
          } catch {
            case ex: Exception =>
              val qsMap = FormUtil.parseQsToMap(qs)
              MImg.qsbStandalone
                .bind("i", qsMap)
                .flatMap { e => if (e.isLeft) None else Some(e.right.get) }
          }
        },
        { _.fold("")(_.fileName) }
      )
      .verifying("error.invalid", _.isDefined)
      .transform [MImg] (_.get, Some.apply)
  )


  /** Рендер главной страницы sys.img-раздела админки. */
  def index(q: Option[String]) = IsSuperuser { implicit request =>
    val imgs = Seq.empty[MImg]
    // TODO Нужно искать все картинки или по указанному в q запросу.
    Ok( indexTpl(imgs, imgFormM) )
  }

  /**
   * Сабмит сырого текста, описывающего картинку.
   * @return Редирект на urlFormSubmitGet(), если есть подходящая картинка. Или какую-то иную инфу.
   */
  def searchFormSubmit = IsSuperuser { implicit request =>
    imgFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("searchFormSubmit(): Failed to bind search form:\n " + formatFormErrors(formWithErrors))
        NotAcceptable( indexTpl(Seq.empty, formWithErrors) )
      },
      {img =>
        Redirect( routes.SysImg.showOne(img) )
      }
    )
  }

  /**
   * Сабмит формы поиска картинки по ссылке на неё.
   * @param im Данные по запрашиваемой картинке.
   */
  def showOne(im: MImg) = IsSuperuser.async { implicit request =>
    val metaFut = im.permMetaCached
    // TODO Искать, где используется эта картинка.
    metaFut map { metaOpt =>
      Ok(showOneTpl(im, metaOpt))
    }
  }

  /**
   * Удалить указанную картинку из хранилища.
   * @param im Картинка.
   * @return Редирект.
   */
  def deleteOneSubmit(im: MImg) = IsSuperuser.async { implicit request =>
    // TODO Удалять на ВСЕХ НОДАХ из кеша /picture/local/
    im.delete map { _ =>
      val (msg, rdr) = if (im.isOriginal) {
        "Оригинал удалён." -> routes.SysImg.index()
      } else {
        "TODO Дериватив может остаться локально на других узлах." -> routes.SysImg.showOne(im.original)
      }
      Redirect(rdr)
        .flashing("success" -> msg)
    }
  }

}
