package util.blocks

import controllers.routes
import io.suggest.ym.model.common.MImgInfoT
import play.api.mvc.Call
import util.img._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import util.blocks.BlocksUtil.BlockImgMap
import play.api.data.{FormError, Mapping}
import models.{Context, MImgInfoMeta, Imgs_t}
import play.api.Play.{current, configuration}

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

  override def getBgImg(bim: BlockImgMap): Option[ImgInfo4Save[ImgIdKey]]  = {
    bim.get(BG_IMG_FN)
  }

  def bgImgCall(imgInfo: MImgInfoT)(implicit ctx: Context): Call = {
    ctx.deviceScreenOpt.fold [Call] {
      routes.Img.getImg(imgInfo.filename)
    } { screen =>
      // TODO тут нужна ссылка на dynImg
      routes.Img.getImg(imgInfo.filename)
    }
  }
}

trait BgImg extends ValT with SaveBgImgI {
  // Константы можно легко переопределить т.к. trait и early initializers.
  def BG_IMG_FN = BgImg.BG_IMG_FN
  def bgImgBf = BgImg.bgImgBf

  override def _saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t, blockHeight: Int): Future[Imgs_t] = {
    import BgImg.{FINAL_DOWNSIZE_SIDE_REL => c}
    val supImgsFut = super._saveImgs(newImgs, oldImgs, blockHeight)
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
}

