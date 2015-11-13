package util.blocks

import io.suggest.model.n2.edge.{MEdgeInfo, MEdge}
import models.blk.ed.{BimKey_t, BindAcc, BlockImgMap, ImgsEmpty, Imgs_t}
import models.im.MImg3_
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.img._

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
  final def saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t = ImgsEmpty): Future[Imgs_t] = {
    _saveImgs(newImgs, oldImgs)
  }

  /** Привести результат bind'а формы к виду сохраненных картинок. */
  final def asSavedImgs(newImgs: BlockImgMap, oldImgs: Imgs_t = ImgsEmpty): Future[Imgs_t] = {
    val imgKeys = imgKeys

    // Лениво выкинуть из исходника все картинки, поддерживаемые этим блоком.
    val oldImgs2Iter: Iterator[MEdge] = {
      val iter = oldImgs.iterator
      if (oldImgs.nonEmpty && imgKeys.nonEmpty) {
        iter.filterNot { e =>
          imgKeys.contains( e.predicate )
        }
      } else {
        iter
      }
    }

    // Собрать новые эджи картинок на основе переданных картинок
    val newImgsIter: Iterator[MEdge] = {
      newImgs
        .iterator
        .map { case (pred, mimg) =>
          MEdge(
            nodeId = mimg.rowKeyStr,
            predicate = pred,
            info = MEdgeInfo(
              dynImgArgs = mimg.qOpt
            )
          )
        }
    }

    // Вернуть результат.
    val res = (oldImgs2Iter ++ newImgsIter).toList
    Future.successful(res)
  }

  /** Все ключи картинок в рамках блока. Используется для фильтрации oldImgs.
    * Трейты поддержки img-полей должны override'ть этот метод, закидывая туда свой ключ. */
  def imgKeys: List[BimKey_t] = Nil

  /** Метод, выполняющий необходимые обновления картинки. Должен быть перезаписан в конкретных подреализациях. */
  protected def _saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t): Future[Imgs_t] = {
    Future.successful( ImgsEmpty )
  }

}


/** Базовая утиль для работы с картинками из blocks-контекстов. */
object SaveImgUtil extends MergeBindAcc[BlockImgMap] {

  // TODO DI, портировать на DI модель BlockConf, а затем и это.
  import play.api.Play.current
  protected[blocks] val mImg3 = current.injector.instanceOf[MImg3_]

  def saveImgsStatic(fn: BimKey_t, newImgs: BlockImgMap, oldImgs: Imgs_t, supImgsFut: Future[Imgs_t]): Future[Imgs_t] = {
    val needImgsThis = newImgs.get(fn)
    val oldImgsThis = oldImgs
      .find(_.predicate == fn)
      .map { mImg3.apply }
    // Нанооптимизация: не ворочить картинками, если нет по ним никакой инфы.
    if (needImgsThis.isDefined || oldImgsThis.isDefined) {
      // Есть картинки для обработки (старые или новые), запустить обработку.
      val saveBgImgFut = ImgFormUtil.updateOrigImgFull(
          needImgs = needImgsThis.toSeq,
          oldImgs  = oldImgsThis.toIterable
        )
        .map(_.headOption)

      val imgInfoOptFut = saveBgImgFut.map { savedBgImg =>
        savedBgImg.map { mimg =>
          MEdge(fn, mimg.rowKeyStr, info = MEdgeInfo(
            dynImgArgs = mimg.qOpt
          ))
        }
      }

      for {
        imgInfoOpt  <- imgInfoOptFut
        supSavedMap <- supImgsFut
      } yield {
        imgInfoOpt.fold(supSavedMap) { imgInfo =>
          imgInfo :: supSavedMap
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

