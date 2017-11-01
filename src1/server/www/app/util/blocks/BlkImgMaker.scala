package util.blocks

import javax.inject.{Inject, Singleton}

import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.util.logs.MacroLogsImpl
import models.blk.{SzMult_t, szMulted}
import models.im._
import models.im.make.{IMakeArgs, IMaker, MakeResult}

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
                              implicit private val ec: ExecutionContext
                            )
  extends IMaker
  with MacroLogsImpl
{


  /**
   * Вычислить размер картинки для рендера на основе размера блока и параметрах экрана.
   * @param szMult Желаемый контроллером множитель размера картинки.
   * @param blockMeta Целевой размер. В частности - метаданные блока.
   * @return Параметры для картинки.
   */
  private def getRenderSz(szMult: SzMult_t, blockMeta: ISize2di, pxRatio: DevPixelRatio): MSize2di = {
    val imgResMult = pxRatio.pixelRatio * szMult
    MSize2di(
      height = szMulted(blockMeta.height, imgResMult),
      width  = szMulted(blockMeta.width, imgResMult)
    )
  }


  /** Этот движок внутри работает синхронно. Его синхронный код вынесен сюда. */
  private def icompileSync(args: IMakeArgs): MakeResult = {
    import args._

    val pxRatio = DevPixelRatios.pxRatioDefaulted( devScreenOpt.flatMap(_.pixelRatioOpt) )

    // Компрессия выходной картинки, желательно как fg её сжимать.
    val fgc = args.compressMode
      .getOrElse(CompressModes.Fg)
      .fromDpr(pxRatio)

    // Настройки сохранения результирующей картинки (аккамулятор).
    var imOpsAcc: List[ImOp] = List(
      StripOp,
      ImInterlace.Plane,
      fgc.chromaSubSampling,
      fgc.imQualityOp
    )

    // Втыкаем resize. Он должен идти после возможного кропа, но перед другими операциями.
    val szReal = getRenderSz(szMult, blockMeta, pxRatio)
    imOpsAcc ::= {
      // IgnoreAspectRatio полезен, иначе браузер сам начнёт пытаться растягивать картинку, отображая мазню на экране.
      AbsResizeOp(szReal, ImResizeFlags.IgnoreAspectRatio)
    }
    imOpsAcc ::= ImFilters.Lanczos

    // Генерим финальную ссыль на картинку с учетом возможного кропа или иных исходных трансформаций:
    val dargs = img.withDynOps(img.dynImgOps ++ imOpsAcc)
    MakeResult(
      szCss       = args.blockMeta,
      szReal      = szReal,
      dynCallArgs = dargs,
      isWide      = false
    )
  }

  /**
   * img compile - собрать ссылку на изображение и сопутствующие метаданные.
   * @param args Контейнер с данными для вызова.
   * @return Фьючерс с экземпляром MakeResult.
   */
  override def icompile(args: IMakeArgs): Future[MakeResult] = {
    // Раз системе надо асинхронно, значит делаем асинхронно в принудительном порядке:
    Future {
      icompileSync(args)
    }
  }

}

/** Интерфейс для поля с DI-инстансом maker'а [[BlkImgMaker]]. */
trait IBlkImgMakerDI {
  def blkImgMaker: IMaker
}
