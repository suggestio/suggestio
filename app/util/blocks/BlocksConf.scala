package util.blocks

import play.api.templates._
import play.api.data._, Forms._
import BlocksUtil._
import util.FormUtil._
import views.html.blocks._
import models._
import io.suggest.ym.model.ad.EMAdOffers
import io.suggest.ym.model.common.BlockMeta
import util.img.{OrigImageUtil, ImgInfo4Save, ImgFormUtil, ImgIdKey}
import io.suggest.ym.model.common.EMImg.Imgs_t
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import util.blocks.BlocksUtil.BlockImgMap

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.14 16:50
 * Description: Конфиги для блоков верстки.
 */

object BlocksConf extends Enumeration {

  /** Всё описание блока идёт через наследование Val. */
  protected abstract class Val(id: Int, name: String) extends super.Val(id, name) {
    /** Шаблон для рендера. */
    def template: Template3[MAdT, Boolean, Context, HtmlFormat.Appendable]

    /** Набор маппингов для обработки данных от формы. */
    def strictMapping: Mapping[BlockMapperResult]

    def saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t): Future[Imgs_t]

    /** Более удобный интерфейс для метода template.render(). */
    def renderBlock(mad: MAdT, isStandalone: Boolean)(implicit ctx: Context) = {
      template.render(mad, isStandalone, ctx)
    }

    /**
     * label'ы опций конфига блока, прописанные в conf/messages*.
     * @param bk исходный BK_-идентификатор
     * @return идентификатор, пригодный для резолва через Messages().
      */
    def i18nLabelOf(bk: String) = I18N_PREFIX + bk

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    def blockFields: List[BlockFieldT]

    def blockFieldForName(n: String): Option[BlockFieldT] = {
      blockFields.find(_.name equalsIgnoreCase n)
    }

    /** Отрендерить редактор. */
    def renderEditor(af: Form[_])(implicit ctx: util.Context): HtmlFormat.Appendable = {
      editor._blockEditorTpl(af, withBC = Some(this))
    }
  }

  type BlockConf = Val
  implicit def value2val(x: Value): BlockConf = x.asInstanceOf[BlockConf]



  // Начало значений

  val Block1 = new Val(1, "photoAdnPrice") with SaveBgImg {
    val BG_IMG_FN = "bgImg"
    val BG_IMG_MARKER = BG_IMG_FN + id
    val bgImg = BfImage(BG_IMG_FN, marker = BG_IMG_MARKER, imgUtil = OrigImageUtil)

    val heightField = BfInt(BlockMeta.HEIGHT_ESFN, BlocksEditorFields.Height, minValue = 300, maxValue=460, defaultValue = Some(300))
    val text1Field = BfText(EMAdOffers.TEXT1_ESFN, BlocksEditorFields.TextArea, minLen = 1, maxLen = 64)
    val priceField = BfPrice(EMAdOffers.PRICE_ESFN, BlocksEditorFields.Price)
    val oldPriceField = BfPrice(EMAdOffers.OLD_PRICE_ESFN, BlocksEditorFields.Price)

    override val blockFields = List(
      bgImg, heightField, text1Field, priceField, oldPriceField
    )

    /** Набор маппингов для обработки данных от формы. */
    override val strictMapping = mapping(
      bgImg.getStrictMappingKV,
      heightField.getStrictMappingKV,
      text1Field.getStrictMappingKV,
      priceField.getStrictMappingKV,
      oldPriceField.getOptionalStrictMappingKV
    )
    {(bgIik, height, text1, price, oldPrice) =>
      val bd: BlockData = BlockDataImpl(
        blockMeta = BlockMeta(
          height = height,
          blockId = id
        ),
        offers = List( AOBlock(
          n = 0,
          text1 = Some(text1),
          price = Some(price),
          oldPrice = oldPrice
        ) )
      )
      val bim = Map(bgImg.name -> bgIik)
      BlockMapperResult(bd, bim)
    }
    {case BlockMapperResult(bd, bim) =>
      bd.offers.headOption.map { offer =>
        val price = offer.price.getOrElse(priceField.anyDefaultValue)
        val text1 = offer.text1.getOrElse(text1Field.anyDefaultValue)
        val bgIik = bim.get(bgImg.name).getOrElse(bgImg.fallbackValue)
        (bgIik, bd.blockMeta.height, text1, price, offer.oldPrice)
      }
    }

    /** Шаблон для рендера. */
    override def template = _block1Tpl

    override def i18nLabelOf(bk: String): String = {
      if (bk == text1Field.name) {
        I18N_PREFIX + "title"
      } else {
        super.i18nLabelOf(bk)
      }
    }
  }


  val Block2 = new Val(2, "saleWithText") with SaveBgImg {
    val BG_IMG_FN = "bgImg"
    val bgImg = BfImage(BG_IMG_FN, marker = BG_IMG_FN + id, imgUtil = OrigImageUtil)

    val heightField = BfInt(BlockMeta.HEIGHT_ESFN, BlocksEditorFields.Height, minValue = 140, maxValue=460, defaultValue = Some(140))
    val text1Field = BfText(EMAdOffers.TEXT1_ESFN, BlocksEditorFields.InputText, maxLen = 512)
    val text2Field = BfText(EMAdOffers.TEXT2_ESFN, BlocksEditorFields.TextArea, maxLen = 8192)

    override val blockFields = List(
      bgImg, heightField, text1Field, text2Field
    )

    /** Набор маппингов для обработки данных от формы. */
    override val strictMapping = mapping(
      bgImg.getStrictMappingKV,
      heightField.getStrictMappingKV,
      text1Field.getStrictMappingKV,
      text2Field.getStrictMappingKV
    )
    {(bgIik, height, text1, text2) =>
      val bd: BlockData = BlockDataImpl(
        blockMeta = BlockMeta(
          height = height,
          blockId = id
        ),
        offers = List( AOBlock(
          n = 0,
          text1 = Some(text1),
          text2 = Some(text2)
        ) )
      )
      val bim = Map(bgImg.name -> bgIik)
      BlockMapperResult(bd, bim)
    }
    {case BlockMapperResult(bd, bim) =>
      bd.offers.headOption.map { offer =>
        val text1 = offer.text1.getOrElse(text1Field.anyDefaultValue)
        val text2 = offer.text2.getOrElse(text2Field.anyDefaultValue)
        val bgIik = bim.get(bgImg.name).getOrElse(bgImg.fallbackValue)
        (bgIik, bd.blockMeta.height, text1, text2)
      }
    }

    /** Шаблон для рендера. */
    override def template = _block2Tpl

  }


  // Хелперы

  def maybeWithName(n: String): Option[BlockConf] = {
    try {
      Some(withName(n))
    } catch {
      case ex: NoSuchElementException => None
    }
  }

}


case class BlockMapperResult(bd: BlockData, bim: BlockImgMap)



/** Функционал для сохранения единственной картинки блока. */
protected trait SaveBgImg {
  val BG_IMG_FN: String

  def saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t): Future[Imgs_t] = {
    ImgFormUtil.updateOrigImg(
      needImgs = newImgs.get(BG_IMG_FN).map(ImgInfo4Save(_)),
      oldImgs = oldImgs.get(BG_IMG_FN)
    ) map { savedImgs =>
      savedImgs.headOption
        .map { BG_IMG_FN -> }
        .toMap
    }
  }
}

