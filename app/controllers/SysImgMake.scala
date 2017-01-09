package controllers

import com.google.inject.Inject
import models.blk.BlockMeta
import models.blk.{BlockWidths, BlockHeights, BlockMetaUtil}
import models.im.{MImgT, CompressModes, DevScreen}
import models.im.make.{Makers, MakeArgs, IMakeArgs, SysForm_t}
import models.mctx.Context
import play.api.data.{Form, Mapping}
import play.api.mvc.Result
import util.blocks.BlocksConf
import util.{FormUtil, PlayMacroLogsI}
import util.acl.IsSuperuser
import views.html.sys1.img.make._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.04.15 22:50
 * Description: Аддон для SysImg-контроллера, добавляющий экшены для отладки make-движков.
 */
class SysImgMakeUtil @Inject() (
  blockMetaUtil       : BlockMetaUtil
) {

  import play.api.data.Forms._

  /** Маппинг для [[models.im.make.IMakeArgs]] под нужды этого контроллера. */
  def makeArgsM(img: MImgT): Mapping[IMakeArgs] = {
    mapping(
      "blockMeta" -> blockMetaUtil.imapping,
      "szMult"    -> FormUtil.szMultM,
      "devScreen" -> optional(DevScreen.mappingFat),
      "compress"  -> CompressModes.mappingOpt
    )
    { MakeArgs(img, _, _, _, _) : IMakeArgs }
    {ima =>
      Some((ima.blockMeta, ima.szMult, ima.devScreenOpt, ima.compressMode))
    }
  }

  /** Маппинг формы, с которой работают в шаблоны и контроллеры. */
  def makeFormM(img: MImgT): SysForm_t = {
    Form(tuple(
      "maker" -> Makers.mapping,
      "args"  -> makeArgsM(img)
    ))
  }

}



trait SysImgMake
  extends SioController
  with PlayMacroLogsI
  with IsSuperuser
{

  import mCommonDi._

  def sysImgMakeUtil : SysImgMakeUtil

  /**
   * Рендер страницы с формой задания произвольных параметров вызова maker'а для указанного изображения.
   * @param img Обрабатываемая картинка.
   * @param bmDflt Необязательные дефолтовые данные полей block-meta. Заливаются в начальную форму.
   * @return 200 ok со страницей с формой описания img make-задачи.
   */
  def makeForm(img: MImgT, bmDflt: Option[BlockMeta]) = IsSuGet.async { implicit request =>
    implicit val ctx = implicitly[Context]
    // Забиндить дефолтовые данные в форму
    val form = sysImgMakeUtil.makeFormM(img).fill((
      Makers.StrictWide,
      MakeArgs(
        img = img,
        blockMeta = bmDflt getOrElse {
          BlockMeta(
            blockId = BlocksConf.DEFAULT.id,
            height  = BlockHeights.default.heightPx,
            width   = BlockWidths.default.widthPx,
            wide    = true
          )
        },
        szMult = 1.0F,
        devScreenOpt = ctx.deviceScreenOpt
      )
    ))
    // Запустить рендер страницы с формой
    _makeFormRender(img, form, Ok)(ctx)
  }

  /** Рендер страницы с формой параметров make. */
  private def _makeFormRender(img: MImgT, form: SysForm_t, rs: Status)
                             (implicit ctx: Context): Future[Result] = {
    val makers = Makers.valuesT
    val html = makeFormTpl(img, form, makers)(ctx)
    rs(html)
  }

  /**
   * Сабмит формы make, созданной с помощью makeForm().
   * @param img Обрабатываемая картинка.
   * @return Получившаяся картинка.
   */
  def makeFormSubmit(img: MImgT) = IsSuPost.async { implicit request =>
    sysImgMakeUtil.makeFormM(img).bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"makeFormSubmit(${img.rowKeyStr}): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
        _makeFormRender(img, formWithErrors, NotAcceptable)
      },
      {case (maker, makeArgs) =>
        val imaker = current.injector.instanceOf(maker.makerClass)
        for (makeRes <- imaker.icompile(makeArgs)) yield {
          Redirect(makeRes.dynImgCall)
        }
      }
    )
  }

}
