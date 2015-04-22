package util.blocks

import io.suggest.ym.model.common.{Imgs, MImgInfoT}
import models.blk.SzMult_t
import models.im.DevScreen
import models.im.make.{Makers, MakeArgs, IMakeResult, IMaker}
import util.PlayLazyMacroLogsImpl
import scala.concurrent.{ExecutionContext, Future}
import util.blocks.BlocksUtil.BlockImgMap
import play.api.data.{FormError, Mapping}
import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.14 10:02
 * Description: Утиль для поддержки фоновой картинки блока.
 * 2014.oct.16: Код продолжен толстеть и был перенесён сюда из BlockImg.scala.
 * 2015.apr.17: Тут остался только код поддержки BgImg в терминах блока.
 *   Утиль для генерации фоновых картинок вынесена в BlkImgMaker и ScWideMaker.
 */

object BgImg extends PlayLazyMacroLogsImpl {

  val BG_IMG_FN = "bgImg"
  val bgImgBf = BfImage(BG_IMG_FN, marker = BG_IMG_FN, preDetectMainColor = true)


  /** Быстрый экстрактор фоновой картинки из карточки. */
  def getBgImg(mad: MAdT) = BlocksConf.applyOrDefault(mad.blockMeta.blockId).getMadBgImg(mad)

  /**
   * Часто-дублирующийся код опционального вызова maker'а.
   * @param mad Рекламная карточка.
   * @param maker Движок-изготовитель.
   * @param szMult Мультипликатор размера.
   * @param devScreenOpt Параметры экрана.
   * @return Фьючерс с результатом подготовки изображения.
   */
  def maybeMakeBgImgWith(mad: MAdT, maker: IMaker, szMult: SzMult_t, devScreenOpt: Option[DevScreen])
                    (implicit ec: ExecutionContext): Future[Option[IMakeResult]] = {
    BgImg.getBgImg(mad).fold {
      Future successful Option.empty[IMakeResult]
    } { bgImg =>
      val iArgs = MakeArgs(bgImg, mad.blockMeta, szMult = szMult, devScreenOpt)
      maker.icompile(iArgs)
        .map { Some.apply }
    }
  }

  def maybeMakeBgImg(mad: MAdT, szMult: SzMult_t, devScreenOpt: Option[DevScreen])
                    (implicit ec: ExecutionContext): Future[Option[IMakeResult]] = {
    val maker = if (mad.blockMeta.wide)
      Makers.ScWide
    else
      Makers.Block
    maybeMakeBgImgWith(mad, maker, szMult, devScreenOpt)
  }
  

}


/** Функционал для сохранения фоновой (основной) картинки блока. */
trait SaveBgImgI extends ISaveImgs {

  def BG_IMG_FN: String
  def bgImgBf: BfImage

  /** Прочитать данные по картинки из imgs-поля рекламной карточки. */
  def getMadBgImg(mad: Imgs): Option[MImgInfoT] = {
    mad.imgs.get(BG_IMG_FN)
  }

}


/** Примесь для блока, чтобы в нём появилась поддержка задания/отображения фоновой картинки. */
trait BgImg extends ValT with SaveBgImgI {
  // Константы можно легко переопределить т.к. trait и early initializers.
  def BG_IMG_FN = BgImg.BG_IMG_FN
  def bgImgBf = BgImg.bgImgBf


  /** Поиск поля картинки для указанного имени поля. */
  override def getImgFieldForName(fn: String): Option[BfImage] = {
    if (fn == BG_IMG_FN)
      Some(bgImgBf)
    else
      super.getImgFieldForName(fn)
  }

  override def _saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t, blockHeight: Int): Future[Imgs_t] = {
    val supImgsFut = super._saveImgs(newImgs, oldImgs, blockHeight)
    SaveImgUtil.saveImgsStatic(
      fn = BG_IMG_FN,
      newImgs = newImgs,
      oldImgs = oldImgs,
      supImgsFut = supImgsFut
    )
  }

  abstract override def blockFieldsRev(af: AdFormM): List[BlockFieldT] = bgImgBf :: super.blockFieldsRev(af)

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

