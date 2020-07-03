package util.blocks

import javax.inject.{Inject, Singleton}
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.dev.{MPxRatio, MPxRatios}
import models.blk.{SzMult_t, szMulted}
import models.im._
import models.im.make.{MImgMakeArgs, MakeResult}
import util.img.{IImgMaker, ImgMakerUtil}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.04.15 17:46
 * Description: Движок генерации картинок строго под фон и размер блока.
 * Обычно за основу картинки берется кроп фона, заданный в редакторе.
 *
 * 2017-11-01 (после f328488a592a): Убрана любая корректировка szMult, упрощён и почищен код.
 */
@Singleton
class BlkImgMaker @Inject() (
                              imgMakerUtil              : ImgMakerUtil,
                              implicit private val ec   : ExecutionContext
                            )
  extends IImgMaker
{


  /**
   * Вычислить размер картинки для рендера на основе размера блока и параметрах экрана.
   * @param szMult Желаемый контроллером множитель размера картинки.
   * @param blockMeta Целевой размер. В частности - метаданные блока.
   * @return Параметры для картинки.
   */
  private def getRenderSz(szMult: SzMult_t, blockMeta: ISize2di, pxRatio: MPxRatio): MSize2di = {
    val imgResMult = pxRatio.pixelRatio * szMult
    MSize2di(
      height = szMulted(blockMeta.height, imgResMult),
      width  = szMulted(blockMeta.width, imgResMult)
    )
  }


  /**
   * img compile - собрать ссылку на изображение и сопутствующие метаданные.
   * @param args Контейнер с данными для вызова.
   * @return Фьючерс с экземпляром MakeResult.
   */
  override def icompile(args: MImgMakeArgs): Future[MakeResult] = {
    val origImgId = args.img.dynImgId.original
    val outFmt = origImgId.imgFormat.get

    if (outFmt.isVector) {
      // Это SVG, а convert на выходе выдаёт растр. Надо кропать прямо на клиенте, а не здесь.
      imgMakerUtil.returnImg(origImgId)

    } else {
      // Раз системе надо асинхронно, значит делаем асинхронно в принудительном порядке:
      val pxRatio = MPxRatios.pxRatioDefaulted( args.devScreenOpt.map(_.pxRatio) )

      val compMode = args.compressMode
        .getOrElse(CompressModes.Fg)

      // Компрессия выходной картинки, желательно как fg её сжимать.
      val fgc = ImCompression.forPxRatio(compMode, pxRatio)

      val szReal = getRenderSz(args.szMult, args.targetSz, pxRatio)

      // Это jpeg/png/gif и т.д. Прогнать через convert.
      // Настройки сохранения результирующей картинки (аккамулятор).
      val imOpsAcc =
        ImGravities.Center ::
        BackgroundOp( None ) ::
        AbsResizeOp(szReal, ImResizeFlags.FillArea) ::
        ExtentOp(szReal) ::
        ImFilters.Lanczos ::
        StripOp ::
        ImInterlaces.Plane ::
        fgc.toOps( outFmt )

      val mr = MakeResult(
        szCss       = MSize2di(args.targetSz),
        szReal      = szReal,
        // Генерим финальную ссыль на картинку с учетом возможного кропа или иных исходных трансформаций:
        dynCallArgs = args.img.withDynOps(args.img.dynImgId.imgOps ++ imOpsAcc),
        isWide      = false
      )
      Future.successful(mr)

    }
  }

}

/** Интерфейс для поля с DI-инстансом maker'а [[BlkImgMaker]]. */
trait IBlkImgMakerDI {
  def blkImgMaker: IImgMaker
}
