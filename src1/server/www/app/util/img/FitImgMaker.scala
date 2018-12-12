package util.img

import javax.inject.{Inject, Singleton}
import io.suggest.common.geom.d2.{IHeight, ISize2di, IWidth, MSize2di}
import io.suggest.dev.MPxRatios
import io.suggest.util.logs.MacroLogsImpl
import models.im._
import models.im.make.{MImgMakeArgs, MakeResult}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.18 10:05
  * Description: Сборка ссылок на изображения, которые нужно вписать в контейнер указанного размера.
  * Т.е. вместо fillArea здесь политика fit и пусть один из размеров будет меньше ожидаемого.
  */
@Singleton
class FitImgMaker @Inject()(
                           dynImgUtil                 : DynImgUtil,
                           imgMakerUtil               : ImgMakerUtil,
                           implicit private val ec    : ExecutionContext
                         )
  extends IImgMaker
  with MacroLogsImpl
{

  override def icompile(args: MImgMakeArgs): Future[MakeResult] = {
    // Есть два варианта: подгонка по высоте или по ширине. Т.е. подгонка по абсолютно-меньшей стороне.
    // В image-magick будет передана только одна сторону, а вторая сторона выставилась автоматом на стороне IM.
    // Поэтому сначала определяем меньшую сторону.
    // В MakeResult вторичная сторона будет рассчитана согласно пропорции.

    val origImgId = args.img.dynImgId.original
    val outFmt = origImgId.dynFormat

    // Для SVG ничего делать не надо.
    if (outFmt.isVector) {
      imgMakerUtil.returnImg( origImgId )

    } else {
      // Это растровое изображение. Готовим инструкции для imagemagick: оценить пропорции изменения размера для двух сторон:
      val origImgWhOptFut = dynImgUtil.getImgWh( args.img.dynImgId )

      for {
        origImgWhOpt <- origImgWhOptFut
      } yield {
        val origImgWh = origImgWhOpt.get

        def __diffRatio(f: ISize2di => Int): Double =
          (f(args.targetSz) - f(origImgWh)).toDouble / f(origImgWh).toDouble

        // Вычисляем относительно приращение ширин и высот:
        val widthF = IWidth.f
        val widthRszDiffRatio  = __diffRatio( widthF )
        val heightF = IHeight.f
        val heightRszDiffRatio = __diffRatio( heightF )

        // Узнаём, какой из двух ресайзов (по ширине или по высоте) приводит к вписыванию картинки в targetSz
        val useWidthRatio = widthRszDiffRatio < heightRszDiffRatio

        val getSzForSzMultF = if (useWidthRatio) widthF
                              else heightF

        // Вычислить фактически szMult для изготавливаемой картинки
        val resizeMult = getSzForSzMultF(args.targetSz).toDouble / getSzForSzMultF(origImgWh).toDouble

        def __mkOutputSz(szMult: Double) = MSize2di(
          width  = Math.round(szMult * (if (useWidthRatio) args.targetSz.width else origImgWh.width * resizeMult)).toInt,
          height = Math.round(szMult * (if (useWidthRatio) origImgWh.height * resizeMult else args.targetSz.height)).toInt
        )

        val outputWhCssPx = __mkOutputSz( args.szMult )

        val pxRatio = args.devScreenOpt
          .fold(MPxRatios.default)(_.pxRatio)

        val outputWhPx    = __mkOutputSz( args.szMult * pxRatio.pixelRatio )

        val compMode = args.compressMode
          .getOrElse(CompressModes.Fg)

        // Компрессия, по возможности использовать передний план, т.к. maker используется для соц.сетей.
        val compression = ImCompression.forPxRatio( compMode, pxRatio )

        // Растр. Собираем набор инструкций для imagemagick.
        val imOps =
          AbsResizeOp(
            MSize2di(
              width  = if (useWidthRatio) outputWhPx.width else 0,
              height = if (useWidthRatio) 0 else outputWhPx.height
            )
          ) ::
          ImFilters.Lanczos ::
          StripOp ::
          ImInterlaces.Plane ::
          compression.toOps( outFmt )

        // Сборка данных для новой картинки:
        val mimg2 = args.img.withDynImgId(
          args.img.dynImgId.addDynImgOps(
            imOps
          )
        )

        LOGGER.trace(s"Done\nSrcImg = ${args.img.dynImgId} with wh0=${ISize2di.wxh(origImgWh)}\n args.szMult=(${args.szMult}) resizeMult=>$resizeMult useWidthRatio?$useWidthRatio\n args.tgSz:${ISize2di.wxh(args.targetSz)}\n resWh = ${ISize2di.wxh(outputWhCssPx)}csspx | resPx:${ISize2di.wxh(outputWhPx)}px\n resImg = ${mimg2.dynImgId}\n devScreen = ${args.devScreenOpt.orNull} with pxRatio=$pxRatio")

        // Результат:
        MakeResult(
          szCss       = outputWhCssPx,
          szReal      = outputWhPx,
          dynCallArgs = mimg2,
          isWide      = false
        )
      }
    }
  }

}
