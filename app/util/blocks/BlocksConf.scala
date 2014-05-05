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

  /** Картинка, название, старая и новая цена. Аналог былого DiscountOffer. */
  val Block1 = new Val(1, "photoAdnPrice") with SaveBgImg {

    val heightField = BfInt(BlockMeta.HEIGHT_ESFN, BlocksEditorFields.Height, minValue = 300, maxValue=460, defaultValue = Some(300))
    val text1Field = BfText("title", BlocksEditorFields.InputText, minLen = 0, maxLen = 64)
    val oldPriceField = BfPrice(EMAdOffers.OLD_PRICE_ESFN, BlocksEditorFields.Price)
    val priceField = BfPrice(EMAdOffers.PRICE_ESFN, BlocksEditorFields.Price)

    override val blockFields = List(
      bgImg, heightField, text1Field, oldPriceField, priceField
    )

    /** Набор маппингов для обработки данных от формы. */
    override val strictMapping = mapping(
      bgImg.getStrictMappingKV,
      heightField.getStrictMappingKV,
      text1Field.getOptionalStrictMappingKV,
      oldPriceField.getOptionalStrictMappingKV,
      priceField.getOptionalStrictMappingKV
    )
    {(bim, height, text1, oldPrice, price) =>
      val bd: BlockData = BlockDataImpl(
        blockMeta = BlockMeta(
          height = height,
          blockId = id
        ),
        offers = List( AOBlock(
          n = 0,
          text1 = text1,
          oldPrice = oldPrice,
          price = price
        ) )
      )
      BlockMapperResult(bd, bim)
    }
    {case BlockMapperResult(bd, bim) =>
      bd.offers.headOption.map { offer =>
        val text1 = offer.text1
        val price = offer.price
        (bim, bd.blockMeta.height, text1, offer.oldPrice, price)
      }
    }

    /** Шаблон для рендера. */
    override def template = _block1Tpl
  }


  /** Блок картинки с двумя текстами. */
  val Block2 = new Val(2, "saleWithText") with SaveBgImg {
    val heightField = BfInt(BlockMeta.HEIGHT_ESFN, BlocksEditorFields.Height, minValue = 300, maxValue=460, defaultValue = Some(300))
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
    {(bim, height, text1, text2) =>
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
      BlockMapperResult(bd, bim)
    }
    {bmr =>
      bmr.bd.offers.headOption.map { offer =>
        val text1 = offer.text1.getOrElse(text1Field.anyDefaultValue)
        val text2 = offer.text2.getOrElse(text2Field.anyDefaultValue)
        (bmr.bim, bmr.bd.blockMeta.height, text1, text2)
      }
    }

    /** Шаблон для рендера. */
    override def template = _block2Tpl
  }


  /** Картинка, три заголовка с тремя ценами. */
  val Block3 = new Val(3, "3prices") with SaveBgImg {
    val TITLE_FN = "title"
    val PRICE_FN = "price"
    val OFFERS_COUNT = 3

    val heightField = BfInt(BlockMeta.HEIGHT_ESFN, BlocksEditorFields.Height, minValue = 300, maxValue=460, defaultValue = Some(300))

    protected def bfText(offerNopt: Option[Int]) = BfText(TITLE_FN, BlocksEditorFields.TextArea, maxLen = 128, offerNopt = offerNopt)
    protected def bfPrice(offerNopt: Option[Int]) = BfPrice(PRICE_FN, BlocksEditorFields.Price, offerNopt = offerNopt)

    /** Генерация описания полей. У нас тут повторяющийся маппинг, поэтому blockFields для редактора генерится без полей-констант. */
    override val blockFields: List[BlockFieldT] = {
      val fns = (1 to OFFERS_COUNT)
        .flatMap { offerN =>
          val offerNopt = Some(offerN)
          val titleBf = bfText(offerNopt)
          val priceBf = bfPrice(offerNopt)
          List(titleBf, priceBf)
        }
        .toList
      bgImg :: heightField :: fns
    }

    /** Маппинг для обработки сабмита формы блока. */
    override val strictMapping: Mapping[BlockMapperResult] = {
      // Поля оффера
      val titleMapping = bfText(None)
      val priceMapping = bfPrice(None)
      // Маппинг для одного элемента (оффера)
      val offerMapping = tuple(
        titleMapping.getOptionalStrictMappingKV,
        priceMapping.getOptionalStrictMappingKV
      )
      // Маппинг для списка офферов.
      val offersMapping = list(offerMapping)
        .verifying("error.too.much", { _.size <= 3 })
        .transform[List[AOBlock]](
          {_.iterator
            .zipWithIndex // Делаем zipWithIndex перед фильтром чтобы сохранять выравнивание на странице (css-классы), если 1 или 2 элемент пропущен.
            .filter {
              case ((titleOpt, priceOpt), _) =>
                titleOpt.isDefined || priceOpt.isDefined
            }
            .map {
              case ((titleOpt, priceOpt), i) =>
                AOBlock(n = i,  text1 = titleOpt,  price = priceOpt)
            }
            .toList
          },
          {_.map { aoBlock =>
            // TODO При нарушении нумерации aoBlock.n надо бы заполнять пустоты автоматом.
            aoBlock.text1 -> aoBlock.price
          }}
        )
      // Маппинг для всего блока.
      mapping(
        bgImg.getStrictMappingKV,
        heightField.getStrictMappingKV,
        "offer" -> offersMapping
      )
      {(bim, height, offers) =>
        val bd: BlockData = BlockDataImpl(
          blockMeta = BlockMeta(
            height = height,
            blockId = id
          ),
          offers = offers
        )
        BlockMapperResult(bd, bim)
      }
      {bmr =>
        Some((bmr.bim, bmr.bd.blockMeta.height, bmr.bd.offers))
      }
    }

    /** Шаблон для рендера. */
    override def template = _block3Tpl
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
  // Константы можно легко переопределить т.к. trait и early initializers.
  val BG_IMG_FN = "bgImg"
  val bgImg = BfImage(BG_IMG_FN, marker = BG_IMG_FN, imgUtil = OrigImageUtil)

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

