package controllers

import io.suggest.ym.model.common.{BlockMeta, IBlockMeta}
import models.Context
import models.blk.{BlockWidths, BlockHeights, BlockMetaUtil}
import models.im.{CompressModes, DevScreen, MImg}
import models.im.make.{Makers, MakeArgs, IMakeArgs, SysForm_t}
import play.api.data.{Form, Mapping}
import play.twirl.api.Html
import util.blocks.BlocksConf
import util.{FormUtil, PlayMacroLogsI}
import util.acl.{IsSuperuserPost, IsSuperuserGet}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import views.html.sys1.img.make._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.04.15 22:50
 * Description: Аддон для SysImg-контроллера, добавляющий экшены для отладки make-движков.
 */
object SysImgMake {

  import play.api.data.Forms._

  /** Маппинг для [[models.im.make.IMakeArgs]] под нужды этого контроллера. */
  def makeArgsM(img: MImg): Mapping[IMakeArgs] = {
    mapping(
      "blockMeta" -> BlockMetaUtil.imapping,
      "szMult"    -> FormUtil.szMultM,
      "devScreen" -> optional(DevScreen.mappingFat),
      "compress"  -> CompressModes.mappingOpt
    )
    { MakeArgs(img, _, _, _, _) : IMakeArgs }
    { ima => Some((ima.blockMeta, ima.szMult, ima.devScreenOpt, ima.compressMode)) }
  }

  /** Маппинг формы, с которой работают в шаблоны и контроллеры. */
  def makeFormM(img: MImg): SysForm_t = {
    Form(tuple(
      "maker" -> Makers.mapping,
      "args"  -> makeArgsM(img)
    ))
  }

}


import SysImgMake._


trait SysImgMake extends SioController with PlayMacroLogsI {

  /**
   * Рендер страницы с формой задания произвольных параметров вызова maker'а для указанного изображения.
   * @param img Обрабатываемая картинка.
   * @param bmDflt Необязательные дефолтовые данные полей block-meta. Заливаются в начальную форму.
   * @return 200 ok со страницей с формой описания img make-задачи.
   */
  def makeForm(img: MImg, bmDflt: Option[IBlockMeta]) = IsSuperuserGet.async { implicit request =>
    implicit val ctx = implicitly[Context]
    // Забиндить дефолтовые данные в форму
    val form = makeFormM(img).fill((
      Makers.StrictWide,
      MakeArgs(
        img = img,
        blockMeta = bmDflt getOrElse BlockMeta(
          blockId = BlocksConf.DEFAULT.id,
          height  = BlockHeights.default.heightPx,
          width   = BlockWidths.default.widthPx,
          wide    = true
        ),
        szMult = 1.0F,
        devScreenOpt = ctx.deviceScreenOpt
      )
    ))
    // Запустить рендер страницы с формой
    _makeFormRender(img, form)(ctx)
      .map { Ok(_) }
  }

  /** Рендер страницы с формой параметров make. */
  private def _makeFormRender(img: MImg, form: SysForm_t)(implicit ctx: Context): Future[Html] = {
    val html = makeFormTpl(img, form)(ctx)
    Future successful html
  }

  /**
   * Сабмит формы make, созданной с помощью makeForm().
   * @param img Обрабатываемая картинка.
   * @return Получившаяся картинка.
   */
  def makeFormSubmit(img: MImg) = IsSuperuserPost.async { implicit request =>
    makeFormM(img).bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"makeFormSubmit(${img.rowKeyStr}): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
        _makeFormRender(img, formWithErrors)
          .map { NotAcceptable(_) }
      },
      {case (maker, makeArgs) =>
        maker.icompile(makeArgs).map { makeRes =>
          Redirect(makeRes.dynImgCall)
        }
      }
    )
  }

}
