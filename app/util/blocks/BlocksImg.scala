package util.blocks

import controllers.routes
import io.suggest.ym.model.common.Imgs
import models.im.MImg
import play.api.mvc.Call
import util.cdn.CdnUtil
import util.img._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
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
        .iterator
        .filter(kv  =>  abandonedOldImgAliases contains kv._1)
        .map { case (k, v)  =>  MImg(v.filename) }
        .toIterable
      if (oldImgsAbandoned.nonEmpty) {
        // Удаляем связанные orig-картинки с помощью updateOrigImg()
        ImgFormUtil.updateOrigImgFull(needImgs = Seq.empty, oldImgs = oldImgsAbandoned)
      }
    }
    resultFut
  }

  /** Метод, выполняющий необходимые обновления картинки. Должен быть перезаписан в конкретных подреализациях. */
  protected def _saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t, blockHeight: Int): Future[Imgs_t] = {
    Future successful Map.empty
  }

  def getBgImg(bim: BlockImgMap): Option[MImg] = None
}


/** Базовая утиль для работы с картинками из blocks-контекстов. */
object SaveImgUtil extends MergeBindAcc[BlockImgMap] {

  def saveImgsStatic(fn: String, newImgs: BlockImgMap, oldImgs: Imgs_t, supImgsFut: Future[Imgs_t]): Future[Imgs_t] = {
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

  def updateAcc(offerN: Int, acc0: BindAcc, bim: BlockImgMap) {
    acc0.bim ++= bim
  }

}



object LogoImg {
  val LOGO_IMG_FN = "logo"
  val logoImgBf = BfImage(LOGO_IMG_FN, marker = LOGO_IMG_FN, preserveFmt = true)  // Запилить отдельный конвертор для логотипов на карточках?
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

