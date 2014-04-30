package util.blocks

import play.api.templates._
import play.api.data._, Forms._
import BlocksUtil._
import util.FormUtil._
import views.html.blocks._
import models._
import io.suggest.ym.model.ad.EMAdOffers
import io.suggest.ym.model.common.BlockMeta

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
    def strictMapping: Mapping[BlockData]

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

    /** Отрендерить редактор. */
    def renderEditor(af: Form[_])(implicit ctx: util.Context): HtmlFormat.Appendable = {
      editor._blockEditorTpl(af, withBC = Some(this))
    }
  }

  type BlockConf = Val
  implicit def value2val(x: Value): BlockConf = x.asInstanceOf[BlockConf]


  // Начало значений

  val Block1 = new Val(1, "photoAdnPrice") {
    val heightField = BfInt(BlockMeta.HEIGHT_ESFN, BlocksEditorFields.Height, minValue = 300, maxValue=460, defaultValue = Some(300))
    val text1Field = BfString(EMAdOffers.TEXT1_ESFN, BlocksEditorFields.TextArea, minLen = 1, maxLen = 64)
    val priceField = BfPrice(EMAdOffers.PRICE_ESFN, BlocksEditorFields.Price)
    val oldPriceField = BfPrice(EMAdOffers.OLD_PRICE_ESFN, BlocksEditorFields.Price)

    override val blockFields = List(
      heightField, text1Field, priceField, oldPriceField
    )

    /** Набор маппингов для обработки данных от формы. */
    override val strictMapping = mapping(
      heightField.getStrictMappingKV,
      text1Field.getStrictMappingKV,
      priceField.getStrictMappingKV,
      oldPriceField.getOptionalStrictMappingKV
    )
    {(height, text1, price, oldPrice) =>
      BlockDataImpl(
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
      ): BlockData
    }
    {bd =>
      bd.offers.headOption.map { offer =>
        val price = offer.price.getOrElse(priceField.anyDefaultValue)
        val text1 = offer.text1.getOrElse(text1Field.anyDefaultValue)
        (bd.blockMeta.height, text1, price, offer.oldPrice)
      }
    }

    /** Шаблон для рендера. */
    override def template = _block1Tpl

  }


  val Block2 = new Val(2, "saleWithText") {
    val heightField = BfInt(BlockMeta.HEIGHT_ESFN, BlocksEditorFields.Height, minValue = 140, maxValue=460, defaultValue = Some(140))
    val text1Field = BfString(EMAdOffers.TEXT1_ESFN, BlocksEditorFields.InputText, maxLen = 512)
    val text2Field = BfString(EMAdOffers.TEXT2_ESFN, BlocksEditorFields.TextArea, maxLen = 8192)

    override val blockFields = List(
      heightField, text1Field, text2Field
    )

    /** Набор маппингов для обработки данных от формы. */
    override val strictMapping = mapping(
      heightField.getStrictMappingKV,
      text1Field.getStrictMappingKV,
      text2Field.getStrictMappingKV
    )
    {(height, text1, text2) =>
      BlockDataImpl(
        blockMeta = BlockMeta(
          height = height,
          blockId = id
        ),
        offers = List( AOBlock(
          n = 0,
          text1 = Some(text1),
          text2 = Some(text2)
        ) )
      ): BlockData
    }
    {bd =>
      bd.offers.headOption.map { offer =>
        val text1 = offer.text1.getOrElse(text1Field.anyDefaultValue)
        val text2 = offer.text2.getOrElse(text2Field.anyDefaultValue)
        (bd.blockMeta.height, text1, text2)
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
