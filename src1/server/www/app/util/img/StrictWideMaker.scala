package util.img

import javax.inject.{Inject, Singleton}

import io.suggest.common.geom.d2.MSize2di
import io.suggest.img.crop.MCrop
import io.suggest.util.logs.MacroLogsImplLazy
import models.blk._
import models.im.make.{MImgMakeArgs, MakeResult}
import models.im._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.04.15 18:45
 * Description: Компилятор ссылки на широкие картинки, который НЕ квантует размеры, а генерирует строго затребованную
 * картинку на основе любой исходной.
 * Этот maker был запилен для решения ряда проблем экспорта карточек в соц.сети, у которых текст уезжал вправо за экран,
 * а слева был избыток площади из-за квантования:
 * - https://pbs.twimg.com/media/CCos2thWIAIDlm5.jpg
 * - https://pbs.twimg.com/media/CCs-V1rUMAA-01P.jpg
 *
 * Для определения размеров используются функции ImageMagic по автоматическому извлечению областей нужных размеров:
 * 1. Изображение ресайзится (обычно вниз, но бывает и вверх) до целевой ширины либо превышающей её,
 *    но совпадающей по высоте с целевой высотой: FillArea
 * 2. Gravity = center, и берётся кроп целевых размеров, упирающийся в результат (1) по высоте или ширине.
 */
@Singleton
class StrictWideMaker @Inject() (
                                  imgMakerUtil  : ImgMakerUtil
                                )
  extends IImgMaker
  with MacroLogsImplLazy
{

  /**
   * Собрать ссылку на изображение и сопутствующие метаданные.
   * @param args Контейнер с аргументами вызова.
   * @return Фьючерс с экземпляром [[models.im.make.MakeResult]].
   */
  override def icompile(args: MImgMakeArgs): Future[MakeResult] = {
    val origImgId = args.img.dynImgId.original
    if (origImgId.dynFormat.isVector) {
      // Это SVG.
      imgMakerUtil.returnImg( origImgId )

    } else {
      // TODO Возможно, следует использовать Future.successful()? Вычисление в целом легковесное.
      // Параметры экрана обязательны при вызове этого maker'а.
      val devScreen = args.devScreenOpt.getOrElse {
        throw new IllegalArgumentException(getClass.getSimpleName + ": args.devScreen is mandatory for this maker. You've passed empty screen info.")
      }
      val pxRatio = devScreen.pixelRatio

      // Нужно вычислить размеры wide-версии оригинала. Используем szMult для вычисления высоты.
      val heightCssRaw = szMultedF(args.targetSz.height, args.szMult)
      val height = szMulted(heightCssRaw, pxRatio.pixelRatio)
      // Ширину экрана берем из DevScreen.
      val widthCssPx = devScreen.width
      val width = szMulted(widthCssPx, pxRatio.pixelRatio)

      // Компрессия, по возможности использовать передний план, т.к. maker используется для соц.сетей.
      val compression = args.compressMode
        .getOrElse(CompressModes.Fg)
        .fromDpr(pxRatio)

      val szReal = MSize2di(height = height, width = width)

      // Растр. Собираем набор инструкций для imagemagick.
      val imOps = List[ImOp](
        ImGravities.Center,
        AbsResizeOp(szReal, ImResizeFlags.FillArea),
        AbsCropOp( MCrop(width = width, height = height, offX = 0, offY = 0) ),
        //ImFilters.Lanczos,
        StripOp,
        ImInterlaces.Plane,
        compression.chromaSubSampling,
        compression.imQualityOp
      )

      val szCss = MSize2di(height = szRounded(heightCssRaw),  width = widthCssPx)

      val mr = MakeResult(
        szCss       = szCss,
        szReal      = szReal,
        dynCallArgs = args.img.withDynOps(imOps),
        isWide      = true
      )
      Future.successful(mr)
    }
  }

}
