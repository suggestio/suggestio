package controllers

import java.net.{MalformedURLException, URL}

import com.google.inject.Inject
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.im.{MAnyImgs, MImg3, MImgT, MImgs3}
import play.api.data.Forms._
import play.api.data._
import util.acl.IsSu
import util.FormUtil
import views.html.sys1.img._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.14 11:40
 * Description: sys-раздел для работы с конкретными картинками.
 * Изначально была потребность быстро получать оригиналы картинок и получать прочую информацию по хранимым
 * изображениям.
 */
class SysImg @Inject() (
  mImgs3                          : MImgs3,
  override val sysImgMakeUtil     : SysImgMakeUtil,
  mImgs                           : MAnyImgs,
  override val isSu               : IsSu,
  override val mCommonDi          : ICommonDi
)
  extends SioControllerImpl
  with MacroLogsImpl
  with SysImgMake
{

  import LOGGER._
  import mCommonDi._

  /** Маппинг для поисковой формы, который пытается распарсить ссылку с img qs или просто img qs. */
  def imgFormM: Form[MImgT] = Form(
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
            case ex: MalformedURLException =>
              LOGGER.trace("imgFormM: qstr contains malformed URL: " + s, ex)
              s
          }
        },
        { identity }
      )
      .transform [Option[MImgT]] (
        {qs =>
          try {
            Some( MImg3(qs) )
          } catch {
            case ex: Exception =>
              LOGGER.trace("imgFormM: qs invalid: " + qs, ex)
              val qsMap = FormUtil.parseQsToMap(qs)
              MImgT.qsbStandalone
                .bind("i", qsMap)
                .flatMap { e => if (e.isLeft) None else Some(e.right.get) }
          }
        },
        { _.fold("")(_.fileName) }
      )
      .verifying("error.invalid", _.isDefined)
      .transform [MImgT] (_.get, Some.apply)
  )


  /** Рендер главной страницы sys.img-раздела админки. */
  def index(q: Option[String]) = csrf.AddToken {
    isSu() { implicit request =>
      val imgs = Seq.empty[MImgT]
      // TODO Нужно искать все картинки или по указанному в q запросу.
      Ok( indexTpl(imgs, imgFormM) )
    }
  }


  /**
   * Сабмит сырого текста, описывающего картинку.
   *
   * @return Редирект на urlFormSubmitGet(), если есть подходящая картинка. Или какую-то иную инфу.
   */
  def searchFormSubmit = csrf.Check {
    isSu() { implicit request =>
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
  }


  /**
   * Сабмит формы поиска картинки по ссылке на неё.
   *
   * @param im Данные по запрашиваемой картинке.
   */
  def showOne(im: MImgT) = csrf.AddToken {
    isSu().async { implicit request =>
      // TODO Искать, где используется эта картинка.
      for (metaOpt <- mImgs3.permMetaCached(im)) yield {
        Ok(showOneTpl(im, metaOpt))
      }
    }
  }


  /**
   * Удалить указанную картинку из хранилища.
   *
   * @param im Картинка.
   * @return Редирект.
   */
  def deleteOneSubmit(im: MImgT) = csrf.Check {
    isSu().async { implicit request =>
      // TODO Удалять на ВСЕХ НОДАХ из кеша /picture/local/
      for (_ <- mImgs.delete(im)) yield {
        val (msg, rdr) = if (im.isOriginal) {
          "Оригинал удалён." -> routes.SysImg.index()
        } else {
          "TODO Дериватив может остаться локально на других узлах." -> routes.SysImg.showOne(im.original)
        }
        Redirect(rdr)
          .flashing(FLASH.SUCCESS -> msg)
      }
    }
  }

}
