package controllers

import io.suggest.ad.blk.BlockMeta
import io.suggest.common.geom.d2.{MSize2di, MSize2diJvm}
import io.suggest.dev.{MScreenJvm, MSzMults}
import io.suggest.util.logs.IMacroLogs
import javax.inject.Singleton
import models.im.{CompressMode, MImgT}
import models.im.make.{MImgMakeArgs, MImgMaker, MImgMakers, SysForm_t}
import models.mctx.Context
import models.mproj.IMCommonDi
import play.api.data.{Form, Mapping}
import play.api.mvc.Result
import util.FormUtil
import util.acl.IIsSu
import util.img.IDynImgUtil
import views.html.sys1.img.make._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.04.15 22:50
 * Description: Аддон для SysImg-контроллера, добавляющий экшены для отладки make-движков.
 */
@Singleton
class SysImgMakeUtil {

  import play.api.data.Forms._

  /** Маппинг для [[models.im.make.MImgMakeArgs]] под нужды этого контроллера. */
  def makeArgsM(img: MImgT): Mapping[MImgMakeArgs] = {
    mapping(
      "blockMeta" -> MSize2diJvm.formMapping,
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



trait SysImgMake
  extends ISioControllerApi
  with IMacroLogs
  with IIsSu
  with IDynImgUtil
  with IMCommonDi
{

  import sioControllerApi._
  import mCommonDi._

  def sysImgMakeUtil : SysImgMakeUtil

  /**
   * Рендер страницы с формой задания произвольных параметров вызова maker'а для указанного изображения.
   * @param img Обрабатываемая картинка.
   * @return 200 ok со страницей с формой описания img make-задачи.
   */
  def makeForm(img: MImgT) = csrf.AddToken {
    isSu().async { implicit request =>
      implicit val ctx = implicitly[Context]
      // Забиндить дефолтовые данные в форму
      val form = sysImgMakeUtil.makeFormM(img).fill((
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
      sysImgMakeUtil.makeFormM(img).bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug(s"makeFormSubmit(${img.dynImgId.rowKeyStr}): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
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

}
