package controllers

import java.net.{MalformedURLException, URL}
import io.suggest.ad.blk.BlockMeta
import io.suggest.common.geom.d2.{MSize2di, MSize2diJvm}
import io.suggest.dev.{MScreenJvm, MSzMults}

import javax.inject.Inject
import io.suggest.util.logs.MacroLogsImplLazy
import models.im.make.{MImgMakeArgs, MImgMaker, MImgMakers, SysForm_t}
import models.im.{CompressMode, MAnyImgs, MImg3, MImgT, MImgs3}
import models.mctx.Context
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.Result
import util.acl.{IsSu, SioControllerApi}
import util.FormUtil
import util.img.DynImgUtil
import views.html.sys1.img._
import views.html.sys1.img.make.makeFormTpl

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.14 11:40
 * Description: sys-раздел для работы с конкретными картинками.
 * Изначально была потребность быстро получать оригиналы картинок и получать прочую информацию по хранимым
 * изображениям.
 */
final class SysImg @Inject() (
                               sioControllerApi   : SioControllerApi,
                             )
  extends MacroLogsImplLazy
{

  import sioControllerApi._
  import mCommonDi.{ec, csrf, current}
  import mCommonDi.current.injector

  private lazy val mImgs3 = injector.instanceOf[MImgs3]
  private lazy val mImgs = injector.instanceOf[MAnyImgs]
  private lazy val isSu = injector.instanceOf[IsSu]
  private lazy val dynImgUtil = injector.instanceOf[DynImgUtil]


  /** Маппинг для поисковой формы, который пытается распарсить ссылку с img qs или просто img qs. */
  private def imgFormM: Form[MImgT] = Form(
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
                .flatMap( _.toOption )
          }
        },
        { _.fold("")(_.dynImgId.fileName) }
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
  def searchFormSubmit() = csrf.Check {
    isSu() { implicit request =>
      imgFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug("searchFormSubmit(): Failed to bind search form:\n " + formatFormErrors(formWithErrors))
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
      for (metaOpt <- mImgs3.imgMetaData(im)) yield {
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
        val (msg, rdr) = if (!im.dynImgId.hasImgOps) {
          "Оригинал удалён." -> routes.SysImg.index()
        } else {
          "TODO Дериватив может остаться локально на других узлах." -> routes.SysImg.showOne(im.original)
        }
        Redirect(rdr)
          .flashing(FLASH.SUCCESS -> msg)
      }
    }
  }



  /**
    * Рендер страницы с формой задания произвольных параметров вызова maker'а для указанного изображения.
    * @param img Обрабатываемая картинка.
    * @return 200 ok со страницей с формой описания img make-задачи.
    */
  def makeForm(img: MImgT) = csrf.AddToken {
    isSu().async { implicit request =>
      implicit val ctx = implicitly[Context]
      // Забиндить дефолтовые данные в форму
      val form = makeFormM(img).fill((
        MImgMakers.StrictWide,
        MImgMakeArgs(
          img = img,
          targetSz = MSize2di(BlockMeta.DEFAULT),
          szMult = MSzMults.`1.0`.toFloat,
          devScreenOpt = ctx.deviceScreenOpt
        )
      ))
      // Запустить рендер страницы с формой
      _makeFormRender(img, form, Ok)(ctx)
    }
  }

  /** Рендер страницы с формой параметров make. */
  private def _makeFormRender(img: MImgT, form: SysForm_t, rs: Status)
                             (implicit ctx: Context): Future[Result] = {
    val makers = MImgMakers.values
    val html = makeFormTpl(img, form, makers)(ctx)
    rs(html)
  }

  /**
    * Сабмит формы make, созданной с помощью makeForm().
    * @param img Обрабатываемая картинка.
    * @return Получившаяся картинка.
    */
  def makeFormSubmit(img: MImgT) = csrf.Check {
    isSu().async { implicit request =>
      makeFormM(img).bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug(s"makeFormSubmit(${img.dynImgId.origNodeId}): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
          _makeFormRender(img, formWithErrors, NotAcceptable)
        },
        {case (maker, makeArgs) =>
          val imaker = current.injector.instanceOf(maker.makerClass)
          for (makeRes <- imaker.icompile(makeArgs)) yield {
            val call = dynImgUtil.imgCall( makeRes.dynCallArgs )
            Redirect( call )
          }
        }
      )
    }
  }


  import play.api.data.Forms._

  /** Маппинг для [[models.im.make.MImgMakeArgs]] под нужды этого контроллера. */
  def makeArgsM(img: MImgT): Mapping[MImgMakeArgs] = {
    mapping(
      "blockMeta" -> MSize2diJvm.size2dFormMapping,
      "szMult"    -> FormUtil.szMultM,
      "devScreen" -> optional(MScreenJvm.mappingFat),
      "compress"  -> CompressMode.mappingOpt
    )
    { MImgMakeArgs(img, _, _, _, _) }
    {ima =>
      Some((ima.targetSz, ima.szMult, ima.devScreenOpt, ima.compressMode))
    }
  }

  /** Маппинг формы, с которой работают в шаблоны и контроллеры. */
  def makeFormM(img: MImgT): SysForm_t = {
    Form(tuple(
      "maker" -> MImgMaker.mapping,
      "args"  -> makeArgsM(img)
    ))
  }


}
