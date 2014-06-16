package util.blocks

import util.img._
import io.suggest.ym.model.common.EMImg.Imgs_t
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import util.blocks.BlocksUtil.BlockImgMap
import play.api.data.{FormError, Mapping}
import models.MImgInfoMeta
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 21:58
 * Description: Blocks-барахло, касающееся картинок. Утиль и элементы конструктора блоков.
 */

/** Интерфейс для сохранения картинок. */
trait ISaveImgs {
  def saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t, blockHeight: Int): Future[Imgs_t] = {
    Future successful Map.empty
  }
}

/** Базовая утиль для работы с картинками из blocks-контекстов. */
object SaveImgUtil {

  def saveImgsStatic(fn: String, newImgs: BlockImgMap, oldImgs: Imgs_t, supImgsFut: Future[Imgs_t],
                     withDownsize: Option[MImgInfoMeta]): Future[Imgs_t] = {
    val needImgsThis0 = newImgs.get(fn)
    val needImgsThis = needImgsThis0.map {
      _.copy(withDownsize = withDownsize)
    }
    val oldImgsThis = oldImgs.get(fn)
    // Нанооптимизация: не ворочить картинками, если нет по ним никакой инфы.
    if (needImgsThis.isDefined || oldImgsThis.isDefined) {
      // Есть картинки для обработки (старые или новые), запустить обработку.
      val saveBgImgFut = ImgFormUtil.updateOrigImg(needImgs = needImgsThis,  oldImgs = oldImgsThis)
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

  /** Закинуть данные по картинке в bindAcc аккамулятор. */
  def mergeBindAccWithBim(maybeAcc: Either[Seq[FormError], BindAcc],
                          maybeImg: Either[Seq[FormError], BlockImgMap]): Either[Seq[FormError], BindAcc] = {
    (maybeAcc, maybeImg) match {
      case (a @ Right(acc0), Right(bim)) =>
        acc0.bim ++= bim
        maybeAcc

      case (Left(accFE), Right(bim)) =>
        maybeAcc

      case (Right(_), Left(colorFE)) =>
        Left(colorFE)   // Избыточна пересборка left either из-за right-типа. Можно также вернуть через .asInstanceOf, но это плохо.

      case (Left(accFE), Left(colorFE)) =>
        Left(accFE ++ colorFE)
    }
  }

}


object BgImg {
  val BG_IMG_FN = "bgImg"
  val bgImgBf = BfImage(BG_IMG_FN, marker = BG_IMG_FN, imgUtil = OrigImageUtil)

  /** Размерность финальной стороны картинки вычисляется на основе соотв. стороны блока.
    * Ширина или высота блока увеличивается на это число, чтобы получить макс.размерность сохраняемой картинки. */
  val FINAL_DOWNSIZE_SIDE_REL = configuration.getDouble("blocks.img.bg.downsize.final.side.rel") getOrElse 2.00000000
}

/** Функционал для сохранения фоновой (основной) картинки блока. */
trait SaveBgImgI extends ISaveImgs {
  def BG_IMG_FN: String
  def bgImgBf: BfImage
}
trait BgImg extends ValT with SaveBgImgI {
  // Константы можно легко переопределить т.к. trait и early initializers.
  def BG_IMG_FN = BgImg.BG_IMG_FN
  def bgImgBf = BgImg.bgImgBf

  override def saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t, blockHeight: Int): Future[Imgs_t] = {
    import BgImg.{FINAL_DOWNSIZE_SIDE_REL => c}
    val supImgsFut = super.saveImgs(newImgs, oldImgs, blockHeight)
    SaveImgUtil.saveImgsStatic(
      fn = BG_IMG_FN,
      newImgs = newImgs,
      oldImgs = oldImgs,
      supImgsFut = supImgsFut,
      withDownsize = Some(MImgInfoMeta(height = (c*blockHeight).toInt, width = (c*blockWidth).toInt))
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
    SaveImgUtil.mergeBindAccWithBim(maybeAcc0, maybeBim)
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
  val logoImgBf = BfImage(LOGO_IMG_FN, marker = LOGO_IMG_FN, imgUtil = AdnLogoImageUtil)  // Запилить отдельный конвертор для логотипов на карточках?
}
/** Функционал для сохранения вторичного логотипа рекламной карточки. */
trait LogoImg extends ValT with ISaveImgs {
  def LOGO_IMG_FN = LogoImg.LOGO_IMG_FN
  def logoImgBf = LogoImg.logoImgBf

  override def saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t, blockHeight: Int): Future[Imgs_t] = {
    val supImgsFut = super.saveImgs(newImgs, oldImgs, blockHeight)
    SaveImgUtil.saveImgsStatic(
      fn = LOGO_IMG_FN,
      newImgs = newImgs,
      oldImgs = oldImgs,
      supImgsFut = supImgsFut,
      withDownsize = None
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
    SaveImgUtil.mergeBindAccWithBim(maybeAcc0, maybeBim)
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
}

