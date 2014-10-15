package util.blocks

import controllers.routes
import io.suggest.ym.model.common.{BlockMeta, Imgs, MImgInfoT}
import models.im._
import play.api.mvc.Call
import util.PlayLazyMacroLogsImpl
import util.cdn.CdnUtil
import util.img._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.annotation.tailrec
import scala.concurrent.Future
import util.blocks.BlocksUtil.BlockImgMap
import play.api.data.{FormError, Mapping}
import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 21:58
 * Description: Blocks-барахло, касающееся картинок. Утиль и элементы конструктора блоков.
 */

/** Интерфейс для сохранения картинок. */
trait ISaveImgs {

  /**
   * Метод для обновления карты картинок. Дергает _saveImgs() и подметает потом за ним.
   * @param newImgs Новая карта картинок.
   * @param oldImgs Старая карта картинок.
   * @param blockHeight Высота блока.
   * @return Новое значение для поля imgs карточки.
   */
  final def saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t, blockHeight: Int): Future[Imgs_t] = {
    val resultFut = _saveImgs(newImgs, oldImgs, blockHeight)
    resultFut onSuccess { case newImgs2 =>
      // 2014.sep.24: Выявлена проблема неудаления картинки. Это происходит если старый алиас ушел из новой карты.
      // Картинка оставалась в хранилище, но на неё терялись все указатели.
      val abandonedOldImgAliases = oldImgs.keySet -- newImgs2.keySet
      val oldImgsAbandoned = oldImgs
        .filterKeys(abandonedOldImgAliases contains)
      if (oldImgsAbandoned.nonEmpty) {
        // Удаляем связанные orig-картинки с помощью updateOrigImg()
        ImgFormUtil.updateOrigImg(needImgs = Seq.empty, oldImgs = oldImgsAbandoned.values)
      }
    }
    resultFut
  }

  /** Метод, выполняющий необходимые обновления картинки. Должен быть перезаписан в конкретных подреализациях. */
  protected def _saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t, blockHeight: Int): Future[Imgs_t] = {
    Future successful Map.empty
  }

  def getBgImg(bim: BlockImgMap): Option[ImgInfo4Save[ImgIdKey]] = None

}


/** Базовая утиль для работы с картинками из blocks-контекстов. */
object SaveImgUtil extends MergeBindAcc[BlockImgMap] {

  def saveImgsStatic(fn: String, newImgs: BlockImgMap, oldImgs: Imgs_t, supImgsFut: Future[Imgs_t]): Future[Imgs_t] = {
    val needImgsThis = newImgs.get(fn)
    val oldImgsThis = oldImgs.get(fn)
    // Нанооптимизация: не ворочить картинками, если нет по ним никакой инфы.
    if (needImgsThis.isDefined || oldImgsThis.isDefined) {
      // Есть картинки для обработки (старые или новые), запустить обработку.
      val saveBgImgFut = ImgFormUtil.updateOrigImg(
        needImgs = needImgsThis.toSeq,
        oldImgs  = oldImgsThis.toIterable
      )
      for {
        savedBgImg <- saveBgImgFut
        supSavedMap <- supImgsFut
      } yield {
        savedBgImg.fold(supSavedMap) {
          savedBgImg =>
            supSavedMap + (fn -> savedBgImg)
        }
      }
    } else {
      // Нет данных по картинкам. Можно спокойно возвращать исходный фьючерс.
      supImgsFut
    }
  }

  def updateAcc(offerN: Int, acc0: BindAcc, bim: BlockImgMap) {
    acc0.bim ++= bim
  }

}


object BgImg extends PlayLazyMacroLogsImpl {

  val BG_IMG_FN = "bgImg"
  val bgImgBf = BfImage(BG_IMG_FN, marker = BG_IMG_FN, imgUtil = OrigImageUtil)

  /**
   * Определить максимальный разумный множитель размера картинки для указанного экрана.
   * Если рендер с указанным множителем не оправдан, то будет попытка с множитель в 2 раза меньшим.
   * @param szMult Текущий (исходный) желаемый множитель размера. Т.е. максимальный допустимый (запрошенный).
   * @param blockSz Размер блока.
   * @param screenSz Размер экрана.
   * @return Множитель.
   */
  @tailrec final def detectMaxSzMult(szMult: Int, blockSz: MImgSizeT, screenSz: MImgSizeT): Int = {
    if (szMult <= 1) {
      1
    } else if (blockSz.width * szMult <= screenSz.width) {
      szMult
    } else {
      detectMaxSzMult(szMult / 2, blockSz, screenSz)
    }
  }

}

/** Функционал для сохранения фоновой (основной) картинки блока. ValT нужен для доступа к blockWidth. */
trait SaveBgImgI extends ISaveImgs with ValT {

  import BgImg.LOGGER._

  def BG_IMG_FN: String
  def bgImgBf: BfImage

  override def getBgImg(bim: BlockImgMap): Option[ImgInfo4Save[ImgIdKey]] = {
    bim.get(BG_IMG_FN)
  }

  /** Сгенерить ссылку для получения фоновой картинки. Система выберет подходящую картинку под девайс.
    * @param imgInfo Инфа о картинке, используемой в качестве фона.
    * @param blockMeta Метаданные блока (и картинки, соответственно).
    * @param brArgs Параметры рендера блока.
    * @param ctx Контекст рендера шаблонов.
    * @return Экземпляр Call, пригодный для рендера в ссылку.
    */
  def bgImgCall(imgInfo: MImgInfoT, blockMeta: BlockMeta, brArgs: BlockRenderArgs)(implicit ctx: Context): Call = {
    Some( ImgIdKey(imgInfo.filename) )
      .filter { iik =>
        // В былом формате откропанная картинка хранилась в двойном разрешении, которое соответствовало размерам блока.
        // Ширина и длина кропа сохранялись согласно двойному размеру блока, а offX и offY были относительно оригинала.
        // В общем, абсолютно неюзабельный для дальнейших трансформаций формат.
        iik.cropOpt.isEmpty || iik.cropOpt.exists { crop =>
          val oldFormat  =  crop.height == blockMeta.height  ||  crop.width == blockMeta.width
          !oldFormat
        }
      }
      .flatMap {
        // dynImg принимает только orig-картинки.
        case oiik: OrigImgIdKey => Some(oiik)
        case _ => None
      }
      .fold [Call] {
        // Пропускаем картинку, ибо данные для этого дела были отброшены.
        CdnUtil.getImg(imgInfo.filename)
      } { oiik =>
        val devPxRatio = ctx.deviceScreenOpt
          .fold(DevPixelRatios.MDPI)(_.pixelRatio)
        // Генерим dynImg-ссылку на картинку.
        val fgc = devPxRatio.fgCompression

        // Определить максимальный мультипликатор размера картинки, сложив запрошенный мультипликатор и размеры экрана.
        val devScrSize: MImgSizeT = ctx.deviceScreenOpt getOrElse {
          warn(s"bgImgCall($imgInfo, bh=${blockMeta.height}): width=${blockMeta.width} Missing client screen size! Will use standard VGA (1024х768)!")
          MImgInfoMeta(width = 1024, height = 768)
        }

        // Настройки сохранения результирующей картинки (аккамулятор).
        var imOpsAcc: List[ImOp] = List(
          StripOp,
          ImInterlace.Plane,
          fgc.chromaSubSampling,
          fgc.imQualityOp
        )

        // Реальный мультипликатор размера (разрешения) картинки на основе размеров экрана, блока и пожеланий в настройках рендера.
        val sizeMult = BgImg.detectMaxSzMult(brArgs.szMult, blockMeta, screenSz = devScrSize)

        // Финальный мультипликатор размера картинки. Учитывает плотность пикселей устройства и допуск рендера в 2х разрешении.
        // TODO Надо наверное как-то ограничивать это чудо природы? Для развернутой картинки на 3.0-экране будет 6-кратное разрешение блока /O_o/
        val imgResMult = devPxRatio.pixelRatio * sizeMult

        // Втыкаем resize. Он должен идти после возможного кропа, но перед другими операциями.
        imOpsAcc ::= {
          val sz = MImgInfoMeta(
            height = (blockMeta.height * imgResMult).toInt,
            width  = (blockMeta.width * imgResMult).toInt
          )
          AbsResizeOp(sz)
        }

        // Если картинка была сохранена откропанной, то надо откропать исходник заново, отресайзив до размера кропа.
        if (oiik.cropOpt.isDefined) {
          val crop = oiik.cropOpt.get
          imOpsAcc ::= AbsCropOp(crop)
        }
        imOpsAcc ::= ImFilters.Lanczos
        // Генерим финальную ссыль на картинку:
        val dargs = DynImgArgs(oiik.uncropped,  imOpsAcc)
        CdnUtil.dynImg(dargs)
      }
  }

}

trait BgImg extends ValT with SaveBgImgI {
  // Константы можно легко переопределить т.к. trait и early initializers.
  def BG_IMG_FN = BgImg.BG_IMG_FN
  def bgImgBf = BgImg.bgImgBf

  override def _saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t, blockHeight: Int): Future[Imgs_t] = {
    val supImgsFut = super._saveImgs(newImgs, oldImgs, blockHeight)
    SaveImgUtil.saveImgsStatic(
      fn = BG_IMG_FN,
      newImgs = newImgs,
      oldImgs = oldImgs,
      supImgsFut = supImgsFut
    )
  }

  abstract override def blockFieldsRev: List[BlockFieldT] = bgImgBf :: super.blockFieldsRev

  // Mapping
  private def m = bgImgBf.getStrictMapping.withPrefix(bgImgBf.name).withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeBim = m.bind(data)
    SaveImgUtil.mergeBindAcc(maybeAcc0, maybeBim)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val v = m.unbind( value.unapplyBIM(bgImgBf) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = value.unapplyBIM(bgImgBf)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}


object LogoImg {
  val LOGO_IMG_FN = "logo"
  val logoImgBf = BfImage(LOGO_IMG_FN, marker = LOGO_IMG_FN, imgUtil = AdnLogoImageUtil, preserveFmt = true)  // Запилить отдельный конвертор для логотипов на карточках?
}

/** Функционал для сохранения вторичного логотипа рекламной карточки. */
trait LogoImg extends ValT with ISaveImgs {
  def LOGO_IMG_FN = LogoImg.LOGO_IMG_FN
  def logoImgBf = LogoImg.logoImgBf

  override def _saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t, blockHeight: Int): Future[Imgs_t] = {
    val supImgsFut = super._saveImgs(newImgs, oldImgs, blockHeight)
    SaveImgUtil.saveImgsStatic(
      fn = LOGO_IMG_FN,
      newImgs = newImgs,
      oldImgs = oldImgs,
      supImgsFut = supImgsFut
    )
  }

  abstract override def blockFieldsRev: List[BlockFieldT] = logoImgBf :: super.blockFieldsRev

  // Mapping
  private def m = logoImgBf.getStrictMapping.withPrefix(logoImgBf.name).withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeBim = m.bind(data)
    SaveImgUtil.mergeBindAcc(maybeAcc0, maybeBim)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val v = m.unbind( value.unapplyBIM(logoImgBf) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = value.unapplyBIM(logoImgBf)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }


  /**
   * Собрать Call к картинке логотипа.
   * @param mad Рекламная карточка.
   * @param default Строка дефолтового путя к ассету.
   * @param ctx Контекст рендера шаблонов.
   * @return Экземпляр Call, пригодный для обращения в ссылку.
   */
  def logoImgCall(mad: Imgs, default: => Option[String] = None)(implicit ctx: Context): Option[Call] = {
    mad.imgs
      .get(LOGO_IMG_FN)
      .map {
        logoImgInfo  =>  CdnUtil.getImg(logoImgInfo.filename)
      }
      .orElse {
        default.map { routes.Assets.versioned(_) }
      }
  }

  /**
   * Собрать Call к картинке логотипа, но по возможности через CDN.
   * @param mad Рекламная карточка.
   * @param default Дефолтовый путь до ассета, если в карточке нет логотипа.
   * @param ctx Контекст рендера шаблона.
   * @return Экземпляр Call.
   */
  def logoImgCdnCall(mad: Imgs, default: => Option[String] = None)(implicit ctx: Context): Option[Call] = {
    logoImgCall(mad, default)
      .map { CdnUtil.forCall }
  }

}

