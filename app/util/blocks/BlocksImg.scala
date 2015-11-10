package util.blocks

import io.suggest.ym.model.common.{MImgInfoMeta, MImgInfo}
import models.blk.ed.BindAcc
import models.blk.ed.{BlockImgMap, BimKey_t, Imgs_t, ImgsEmpty}
import models.im.MImg
import util.img._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

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
   * @return Новое значение для поля imgs карточки.
   */
  final def saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t): Future[Imgs_t] = {
    _saveImgs(newImgs, oldImgs)
  }

  /** Привести результат bind'а формы к виду сохраненных картинок. */
  final def asSavedImgs(newImgs: BlockImgMap, oldImgs: Imgs_t = Map.empty): Future[Imgs_t] = {
    Future.traverse(newImgs) {
      case (k, i4s) =>
        for {
          imgMetaOpt <- i4s.getImageWH
        } yield {
          val mii = MImgInfo(i4s.fileName, meta = imgMetaOpt.map(MImgInfoMeta.apply))
          k -> mii
        }
    } map { res =>
      oldImgs ++ res
    }
  }


  /** Метод, выполняющий необходимые обновления картинки. Должен быть перезаписан в конкретных подреализациях. */
  protected def _saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t): Future[Imgs_t] = {
    Future successful ImgsEmpty
  }

}


/** Базовая утиль для работы с картинками из blocks-контекстов. */
object SaveImgUtil extends MergeBindAcc[BlockImgMap] {

  def saveImgsStatic(fn: BimKey_t, newImgs: BlockImgMap, oldImgs: Imgs_t, supImgsFut: Future[Imgs_t]): Future[Imgs_t] = {
    val needImgsThis = newImgs.get(fn)
    val oldImgsThis = oldImgs.get(fn)
      .map { i => MImg(i.filename) }
    // Нанооптимизация: не ворочить картинками, если нет по ним никакой инфы.
    if (needImgsThis.isDefined || oldImgsThis.isDefined) {
      // Есть картинки для обработки (старые или новые), запустить обработку.
      val saveBgImgFut = ImgFormUtil.updateOrigImgFull(
          needImgs = needImgsThis.toSeq,
          oldImgs  = oldImgsThis.toIterable
        )
        .map(_.headOption)
      val imgInfoOptFut = saveBgImgFut.flatMap { savedBgImg =>
        ImgFormUtil.optImg2OptImgInfo(savedBgImg)
      }
      for {
        imgInfoOpt  <- imgInfoOptFut
        supSavedMap <- supImgsFut
      } yield {
        imgInfoOpt.fold(supSavedMap) {
          imgInfo =>  supSavedMap + (fn -> imgInfo)
        }
      }
    } else {
      // Нет данных по картинкам. Можно спокойно возвращать исходный фьючерс.
      supImgsFut
    }
  }

  def updateAcc(offerN: Int, acc0: BindAcc, bim: BlockImgMap): BindAcc = {
    acc0.copy(
      bim = acc0.bim ++ bim
    )
  }

}

