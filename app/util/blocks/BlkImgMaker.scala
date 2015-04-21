package util.blocks

import models.blk.{SzMult_t, szMulted}
import models.im._
import scala.annotation.tailrec
import models._
import models.im.make.{MakeResult, IMakeResult, IMakeArgs, IMaker}

import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.04.15 17:46
 * Description: Движок генерации картинок строго под фон и размер блока.
 * Обычно за основу картинки берется кроп фона, заданный в редакторе.
 */
object BlkImgMaker extends IMaker {

  /**
   * Вычислить размер картинки для рендера на основе размера блока и параметрах экрана.
   * @param szMult Желаемый контроллером множитель размера картинки.
   * @param blockMeta Целевой размер. В частности - метаданные блока.
   * @param devScreen Данные по экрану устройства.
   * @return Параметры для картинки.
   */
  private def getRenderSz(szMult: SzMult_t, blockMeta: MImgSizeT, devScreen: DevScreenT): MImgInfoMeta = {
    getRenderSz(szMult, blockMeta, devScreen, devScreen.pixelRatio)
  }
  private def getRenderSz(szMult: SzMult_t, blockMeta: MImgSizeT, devScreenSz: MImgSizeT, pxRatio: DevPixelRatio): MImgInfoMeta = {
    val imgResMult = getImgResMult(szMult, blockMeta, devScreenSz, pxRatio)
    MImgInfoMeta(
      height = szMulted(blockMeta.height, imgResMult),
      width  = szMulted(blockMeta.width, imgResMult)
    )
  }


  /**
   * Определить мультипликатор размеров сторон картинки. по сути - комбинация pxRatioDefaulted() и detectMaxSzMult().
   * @param szMult Мультипликатор размера, желаемый контроллером.
   * @param blockMeta Целевой размер картинки.
   * @param devScreenSz Экран устройства.
   * @param pxRatio Плотность пикселей устройства.
   * @return Мультипликатор, на который надо домножать пиксельный размер стороны картинки.
   */
  private def getImgResMult(szMult: SzMult_t, blockMeta: MImgSizeT, devScreenSz: MImgSizeT, pxRatio: DevPixelRatio): SzMult_t = {
    // Реальный мультипликатор размера (разрешения) картинки на основе размеров экрана, блока и пожеланий в настройках рендера.
    val sizeMult = detectMaxSzMult(szMult, blockMeta, screenSz = devScreenSz)
    // Финальный мультипликатор размера картинки. Учитывает плотность пикселей устройства и допуск рендера в 2х разрешении.
    // TODO Надо наверное как-то ограничивать это чудо природы? Для развернутой картинки на 3.0-экране будет
    //      6-кратное разрешение блока /O_o/ Памяти на девайсе может не хватить.
    pxRatio.pixelRatio * sizeMult
  }



  /**
   * Определить максимальный разумный множитель размера картинки для указанного экрана.
   * Если рендер с указанным множителем не оправдан, то будет попытка с множитель в 2 раза меньшим.
   * @param szMult Текущий (исходный) желаемый множитель размера. Т.е. максимальный допустимый (запрошенный).
   * @param blockSz Размер блока.
   * @param screenSz Размер экрана.
   * @return Множитель.
   */
  // TODO Вероятно, этот метод не нужен. Мнение контроллера по вопросам рендера не должно "корректироваться" на нижнем уровне.
  @tailrec private def detectMaxSzMult(szMult: SzMult_t, blockSz: MImgSizeT, screenSz: MImgSizeT): SzMult_t = {
    if (szMult <= 1F) {
      1F
    } else if (blockSz.width * szMult <= screenSz.width) {
      szMult
    } else {
      detectMaxSzMult(szMult / 2F, blockSz, screenSz)
    }
  }

  /** Этот движок внутри работает синхронно. Его синхронный код вынесен сюда. */
  private def icompileSync(args: IMakeArgs): IMakeResult = {
    import args._
    val oiik = MImg(img.filename)
    val devScreen = devScreenOpt getOrElse {
      DevScreen(
        width = 1024,
        height = 768,
        pixelRatioOpt = None
      )
    }
    // Компрессия выходной картинки, желательно как fg её сжимать.
    val fgc = args.compressMode
      .getOrElse(CompressModes.Fg)
      .fromDpr(devScreen.pixelRatio)

    // Настройки сохранения результирующей картинки (аккамулятор).
    var imOpsAcc: List[ImOp] = List(
      StripOp,
      ImInterlace.Plane,
      fgc.chromaSubSampling,
      fgc.imQualityOp
    )

    // Втыкаем resize. Он должен идти после возможного кропа, но перед другими операциями.
    val szReal = getRenderSz(szMult, blockMeta, devScreen)
    imOpsAcc ::= {
      // IgnoreAspectRatio полезен, иначе браузер сам начнёт пытаться растягивать картинку, отображая мазню на экране.
      AbsResizeOp(szReal, ImResizeFlags.IgnoreAspectRatio)
    }
    imOpsAcc ::= ImFilters.Lanczos

    // Генерим финальную ссыль на картинку с учетом возможного кропа или иных исходных трансформаций:
    val dargs = oiik.copy(dynImgOps = oiik.dynImgOps ++ imOpsAcc)
    MakeResult(
      szCss = args.blockMeta,
      szReal = szReal,
      dynCallArgs = dargs,
      isWide = false
    )
  }

  /**
   * img compile - собрать ссылку на изображение и сопутствующие метаданные.
   * @param args Контейнер с данными для вызова.
   * @return Фьючерс с экземпляром MakeResult.
   */
  override def icompile(args: IMakeArgs)(implicit ec: ExecutionContext): Future[IMakeResult] = {
    // Раз системе надо асинхронно, значит делаем асинхронно в принудительном порядке:
    Future {
      icompileSync(args)
    }
  }

}
