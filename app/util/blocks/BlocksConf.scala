package util.blocks

import play.api.templates._
import play.api.data._, Forms._
import BlocksUtil._
import views.html.blocks._
import models._
import io.suggest.ym.model.ad.EMAdOffers
import io.suggest.ym.model.common.BlockMeta
import util.img._
import io.suggest.ym.model.common.EMImg.Imgs_t
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import util.blocks.BlocksUtil.BlockImgMap
import scala.Some

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.14 16:50
 * Description: Конфиги для блоков верстки.
 */

object BlocksConf extends Enumeration {

  /** Всё описание блока идёт через наследование Val и её интерфейса [[ValT]] при необходимости. */
  protected abstract class Val(id: Int, name: String) extends super.Val(id, name) with ValT {
    /**
     * label'ы опций конфига блока, прописанные в conf/messages*.
     * @param bk исходный BK_-идентификатор
     * @return идентификатор, пригодный для резолва через Messages().
      */
    def i18nLabelOf(bk: String) = I18N_PREFIX + bk

    /** Отрендерить редактор. */
    def renderEditor(af: Form[_], formDataSer: Option[String])(implicit ctx: util.Context): HtmlFormat.Appendable = {
      editor._blockEditorTpl(af, withBC = Some(this), formDataSer = formDataSer)
    }

    override def blockFieldsRev: List[BlockFieldT] = Nil
  }


  type BlockConf = Val
  implicit def value2val(x: Value): BlockConf = x.asInstanceOf[BlockConf]

  // Хелперы
  def maybeWithName(n: String): Option[BlockConf] = {
    try {
      Some(withName(n))
    } catch {
      case ex: NoSuchElementException => None
    }
  }


  // Начало значений

  /** Картинка, название, старая и новая цена. Аналог былого DiscountOffer. */
  val Block1 = new Val(1, "photoAdnPrice") with SaveBgImg with HeightStatic {
    val titleBf = BfText("title", BlocksEditorFields.InputText,
      minLen = 0,
      maxLen = 64,
      defaultValue = Some(AOStringField("Платье", AOFieldFont("444444")))
    )
    val oldPriceBf = BfPrice(EMAdOffers.OLD_PRICE_ESFN,
      defaultValue = Some(AOPriceField(200F, "RUB", "200 р.", defaultFont))
    )
    val priceBf = BfPrice(EMAdOffers.PRICE_ESFN,
      defaultValue = Some(AOPriceField(100F, "RUB", "100 р.", defaultFont))
    )

    override def blockFields = List(
      bgImgBf, heightBf, titleBf, oldPriceBf, priceBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping = mapping(
      bgImgBf.getStrictMappingKV,
      heightBf.getStrictMappingKV,
      titleBf.getOptionalStrictMappingKV,
      oldPriceBf.getOptionalStrictMappingKV,
      priceBf.getOptionalStrictMappingKV
    )
    {(bim, height, text1, oldPrice, price) =>
      val blk = AOBlock(
        n = 0,
        text1 = text1,
        oldPrice = oldPrice,
        price = price
      )
      val bd: BlockData = BlockDataImpl(
        blockMeta = getBlockMeta(height),
        offers = List(blk)
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
  val Block2 = new Val(2, "saleWithText") with SaveBgImg with Height {
    override def heightAvailableVals: Set[Int] = Set(300, 460)
    val titleBf = BfText("title", BlocksEditorFields.InputText, maxLen = 512)
    val descrBf = BfText("descr", BlocksEditorFields.TextArea, maxLen = 8192,
      defaultValue = Some(AOStringField("Распродажа. Сегодня. Сейчас.", AOFieldFont("000000")))
    )

    override def blockFields = List(
      bgImgBf, heightBf, titleBf, descrBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping = mapping(
      bgImgBf.getStrictMappingKV,
      heightBf.getStrictMappingKV,
      titleBf.getStrictMappingKV,
      descrBf.getStrictMappingKV
    )
    {(bim, height, text1, text2) =>
      val blk = AOBlock(
        n = 0,
        text1 = Some(text1),
        text2 = Some(text2)
      )
      val bd: BlockData = BlockDataImpl(
        blockMeta = getBlockMeta(height),
        offers = List(blk)
      )
      BlockMapperResult(bd, bim)
    }
    {bmr =>
      bmr.bd.offers.headOption.map { offer =>
        val text1 = offer.text1.getOrElse(titleBf.anyDefaultValue)
        val text2 = offer.text2.getOrElse(descrBf.anyDefaultValue)
        (bmr.bim, bmr.bd.blockMeta.height, text1, text2)
      }
    }

    /** Шаблон для рендера. */
    override def template = _block2Tpl
  }


  /** Блок с тремя ценами в первом дизайне. */
  val Block3 = new Val(3, "3prices") with TitlePriceListBlockT with SaveBgImg with HeightStatic {
    override val offersCount = 3

    /** Генерация описания полей. У нас тут повторяющийся маппинг, поэтому blockFields для редактора генерится без полей-констант. */
    override def blockFields: List[BlockFieldT] = {
      bgImgBf :: heightBf :: super.blockFields
    }

    /** Маппинг для обработки сабмита формы блока. */
    override def strictMapping: Mapping[BlockMapperResult] = {
      // Маппинг для всего блока.
      mapping(
        bgImgBf.getStrictMappingKV,
        heightBf.getStrictMappingKV,
        "offer" -> offersMapping
      )
      {(bim, height, offers) =>
        val bd: BlockData = BlockDataImpl(
          blockMeta = getBlockMeta(height),
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


  sealed abstract class CommonBlock4_9(id: Int, name: String) extends Val(id, name) with SaveBgImg with Height300 {
    val text1Bf = BfText("text1", BlocksEditorFields.InputText, maxLen = 256)
    val priceBf = BfPrice("price")
    val text2Bf = BfText("text2", BlocksEditorFields.TextArea, maxLen = 512)
    val bgColorBf = BfColor("bgColor", defaultValue = Some("0F2841"))
    val borderColorBf = BfColor("borderColor", defaultValue = Some("FFFFFF"))

    val blockWidth: Int

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    override def blockFields: List[BlockFieldT] = List(
      bgImgBf, text1Bf, priceBf, text2Bf, bgColorBf, borderColorBf
    )

    /** Маппинг для обработки данных от сабмита формы блока. */
    override def strictMapping: Mapping[BlockMapperResult] = {
      mapping(
        bgImgBf.getStrictMappingKV,
        text1Bf.getOptionalStrictMappingKV,
        priceBf.getOptionalStrictMappingKV,
        text2Bf.getOptionalStrictMappingKV,
        bgColorBf.getStrictMappingKV,
        borderColorBf.getStrictMappingKV
      )
      {(bim, text1Opt, priceOpt, text2Opt, bgColor, borderColor) =>
        val blk = AOBlock(
          n = 0,
          text1 = text1Opt,
          price = priceOpt,
          text2 = text2Opt
        )
        val bd: BlockData = BlockDataImpl(
          blockMeta = getBlockMeta,
          offers = List(blk),
          colors = Map(
            bgColorBf.name -> bgColor,
            borderColorBf.name -> borderColor
          )
        )
        BlockMapperResult(bd, bim)
      }
      {bmr =>
        val offerOpt = bmr.bd.offers.headOption
        val text1 = offerOpt.flatMap(_.text1)
        val price = offerOpt.flatMap(_.price)
        val text2 = offerOpt.flatMap(_.text2)
        val bgColor = bmr.unapplyColor(bgColorBf)
        val borderColor = bmr.unapplyColor(borderColorBf)
        Some( (bmr.bim, text1, price, text2, bgColor, borderColor) )
      }
    }
  }


  /** Рекламный блок с предложением товара/услуги и рекламным посылом. */
  val Block4 = new CommonBlock4_9(4, "2texts+price") {
    override val blockWidth = 300
    override def template = _block4Tpl
  }


  /** Реклама брендированного товара. От предыдущих одно-офферных блоков отличается дизайном и тем, что есть вторичный логотип. */
  val Block5 = new Val(5, "brandedProduct") with SaveBgImg with SaveLogoImg with HeightStatic {
    val titleBf = BfText("title", BlocksEditorFields.TextArea, maxLen = 256)
    val oldPriceBf = BfPrice("oldPrice")
    val priceBf = BfPrice("price")
    val maskColorBf = BfColor("maskColor", defaultValue = Some("d5c864"))

    override def blockFields: List[BlockFieldT] = List(
      bgImgBf, heightBf, maskColorBf, logoImgBf, titleBf, oldPriceBf, priceBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = mapping(
      maskColorBf.getStrictMappingKV,
      bgImgBf.getStrictMappingKV,
      logoImgBf.getOptionalStrictMappingKV,
      heightBf.getStrictMappingKV,
      titleBf.getOptionalStrictMappingKV,
      oldPriceBf.getOptionalStrictMappingKV,
      priceBf.getOptionalStrictMappingKV
    )
    {(maskColor, bgBim, logoBim, height, text1Opt, oldPriceOpt, priceOpt) =>
      val block = AOBlock(
        n = 0,
        text1 = text1Opt,
        oldPrice = oldPriceOpt,
        price = priceOpt
      )
      val bd: BlockData = BlockDataImpl(
        blockMeta = getBlockMeta(height),
        offers = List(block),
        colors = Map(maskColorBf.name -> maskColor)
      )
      val bim: BlockImgMap = logoBim.fold(bgBim) { bgBim ++ _ }
      BlockMapperResult(bd, bim)
    }
    {bmr =>
      val bgBim = bmr.unapplyBIM(bgImgBf)
      val logoBim = bmr.unapplyBIM(logoImgBf)
      val logoBimOpt = if (logoBim.isEmpty) None else Some(logoBim)
      val height = bmr.bd.blockMeta.height
      val offerOpt = bmr.bd.offers.headOption
      val text1 = offerOpt.flatMap(_.text1)
      val oldPrice = offerOpt.flatMap(_.oldPrice)
      val price = offerOpt.flatMap(_.price)
      val maskColor = bmr.unapplyColor(maskColorBf)
      Some( (maskColor, bgBim, logoBimOpt, height, text1, oldPrice, price) )
    }

    /** Шаблон для рендера. */
    override def template = _block5Tpl
  }


  /** Блок, который содержит до трёх офферов с ценами. Аналог [[Block3]], но с иным дизайном. */
  val Block6 = new Val(6, "3prices2") with TitlePriceListBlockT with SaveBgImg with Height300 {
    override val offersCount = 3

    override def blockFields: List[BlockFieldT] = {
      bgImgBf :: super.blockFields
    }

    override def strictMapping: Mapping[BlockMapperResult] = {
      // Маппинг для всего блока.
      mapping(
        bgImgBf.getStrictMappingKV,
        "offer" -> offersMapping
      )
      {(bim, offers) =>
        val bd: BlockData = BlockDataImpl(
          blockMeta = getBlockMeta,
          offers = offers
        )
        BlockMapperResult(bd, bim)
      }
      {bmr =>
        Some((bmr.bim, bmr.bd.offers))
      }
    }

    override def template = _block6Tpl
  }


  /** Блок, отображающий скидочную цену на товар или услугу. */
  val Block7 = new Val(7, "discountedPrice1") with Height300 {
    val discoBf = BfDiscount("discount", min = -99F, max = 999F)
    val titleBf = BfText("title", BlocksEditorFields.TextArea,
      maxLen = 256,
      defaultValue = Some(AOStringField("", AOFieldFont("444444")))
    )
    val priceBf = BfPrice("price")
    // 2014.may.06: Цвета для слова SALE и фона рамки с %показателем скидки.
    val saleMaskColorBf = BfColor("saleMaskColor", defaultValue = Some("00ff1a"))
    val iconBgColorBf = BfColor("iconBgColor", defaultValue = Some("ff2424"))

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    override def blockFields: List[BlockFieldT] = List(
      saleMaskColorBf, discoBf, titleBf, priceBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = {
      mapping(
        saleMaskColorBf.getStrictMappingKV,
        iconBgColorBf.getStrictMappingKV,
        discoBf.getOptionalStrictMappingKV,
        titleBf.getOptionalStrictMappingKV,
        priceBf.getOptionalStrictMappingKV
      )
      {(bannerFontColor, iconBgColor, discoOpt, titleOpt, priceOpt) =>
        val blk = AOBlock(
          n = 0,
          text1 = titleOpt,
          discount = discoOpt,
          price = priceOpt
        )
        val bd: BlockData = BlockDataImpl(
          blockMeta = getBlockMeta,
          offers = List(blk),
          colors = Map(
            saleMaskColorBf.name -> bannerFontColor,
            iconBgColorBf.name     -> iconBgColor
          )
        )
        val bim: BlockImgMap = Map.empty
        BlockMapperResult(bd, bim)
      }
      {bmr =>
        val offerOpt = bmr.bd.offers.headOption
        val discount = offerOpt.flatMap(_.discount)
        val title = offerOpt.flatMap(_.text1)
        val price = offerOpt.flatMap(_.price)
        val saleMaskColor = bmr.unapplyColor(saleMaskColorBf)
        val iconBgColor = bmr.unapplyColor(iconBgColorBf)
        Some( (saleMaskColor, iconBgColor, discount, title, price) )
      }
    }

    /** Шаблон для рендера. */
    override def template = _block7Tpl
  }


  val Block8 = new Val(8, "titleWithPrice8") with SaveBgImg with Height300 {
    val titleBf = BfText("title", BlocksEditorFields.TextArea, maxLen = 256)
    val priceBf = BfPrice("price")

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    override def blockFields: List[BlockFieldT] = List(
      bgImgBf, titleBf, priceBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = mapping(
      bgImgBf.getStrictMappingKV,
      titleBf.getOptionalStrictMappingKV,
      priceBf.getOptionalStrictMappingKV
    )
    // apply()
    {(bim, titleOpt, priceOpt) =>
      val blk = AOBlock(
        n = 0,
        text1 = titleOpt,
        price = priceOpt
      )
      val bd: BlockData = BlockDataImpl(
        blockMeta = getBlockMeta,
        offers = List(blk)
      )
      BlockMapperResult(bd, bim)
    }
    // unapply()
    {bmr =>
      val bgBim = bmr.unapplyBIM(bgImgBf)
      val offerOpt = bmr.bd.offers.headOption
      val title = offerOpt.flatMap(_.text1)
      val price = offerOpt.flatMap(_.price)
      Some( (bgBim, title, price) )
    }

    /** Шаблон для рендера. */
    override def template = _block8Tpl
  }
  
  
  val Block9 = new CommonBlock4_9(9, "titlePriceDescrNarrow9") {
    override val blockWidth = 140
    override def template = _block9Tpl
  }


  val Block10 = new Val(10, "oldNewPriceNarrow10") with SaveBgImg with Height300 {
    val titleBf     = BfText("title", BlocksEditorFields.TextArea, maxLen = 256)
    val oldPriceBf  = BfPrice("oldPrice")
    val priceBf     = BfPrice("price")

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    override def blockFields: List[BlockFieldT] = List(
      bgImgBf, titleBf, oldPriceBf, priceBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = mapping(
      bgImgBf.getStrictMappingKV,
      titleBf.getOptionalStrictMappingKV,
      oldPriceBf.getOptionalStrictMappingKV,
      priceBf.getOptionalStrictMappingKV
    )
    {(bgBim, titleOpt, oldPriceOpt, priceOpt) =>
      val blk = AOBlock(
        n = 0,
        text1 = titleOpt,
        oldPrice = oldPriceOpt,
        price = priceOpt
      )
      val bd = BlockDataImpl(
        blockMeta = getBlockMeta,
        offers = List(blk)
      )
      BlockMapperResult(bd, bgBim)
    }
    {bmr =>
      val bgBim = bmr.unapplyBIM(bgImgBf)
      val offerOpt = bmr.bd.offers.headOption
      val title = offerOpt.flatMap(_.text1)
      val oldPrice = offerOpt.flatMap(_.oldPrice)
      val price = offerOpt.flatMap(_.price)
      Some( (bgBim, title, oldPrice, price) )
    }

    /** Шаблон для рендера. */
    override def template = _block10Tpl
  }


  val Block11 = new Val(11, "promoNarrow11") with SaveBgImg with Height300 {
    val titleBf = BfText("title", BlocksEditorFields.TextArea, maxLen = 256)
    val descrBf = BfText("descr", BlocksEditorFields.TextArea, maxLen = 256)
    val saleMaskColorBf = BfColor("saleMaskColor", defaultValue = Some("aaaaaa"))

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    override def blockFields: List[BlockFieldT] = List(
      saleMaskColorBf, bgImgBf, titleBf, descrBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = mapping(
      saleMaskColorBf.getStrictMappingKV,
      bgImgBf.getStrictMappingKV,
      titleBf.getOptionalStrictMappingKV,
      descrBf.getOptionalStrictMappingKV
    )
    {(saleMaskColor, bgBim, titleOpt, descrOpt) =>
      val blk = AOBlock(
        n = 0,
        text1 = titleOpt,
        text2 = descrOpt
      )
      val bd = BlockDataImpl(
        blockMeta = getBlockMeta,
        offers = List(blk),
        colors = Map(saleMaskColorBf.name -> saleMaskColor)
      )
      BlockMapperResult(bd, bgBim)
    }
    {bmr =>
      val bgBim = bmr.unapplyBIM(bgImgBf)
      val offerOpt = bmr.bd.offers.headOption
      val title = offerOpt.flatMap(_.text1)
      val descr = offerOpt.flatMap(_.text2)
      val saleMaskColor = bmr.unapplyColor(saleMaskColorBf)
      Some( (saleMaskColor, bgBim, title, descr) )
    }

    /** Шаблон для рендера. */
    override def template = _block11Tpl
  }


  val Block12 = new Val(12, "discountNarrow12") with Height300 {
    val saleMaskColorBf = BfColor("saleMaskColor", defaultValue = Some("00ff1a"))
    val discountBf = BfDiscount("discount", min = -99F, max = 999F)
    val titleBf = BfText("title", BlocksEditorFields.TextArea, maxLen = 256)
    val descrBf = BfText("descr", BlocksEditorFields.TextArea, maxLen = 256)

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    override def blockFields: List[BlockFieldT] = List(
      saleMaskColorBf, discountBf, titleBf, descrBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = mapping(
      saleMaskColorBf.getStrictMappingKV,
      discountBf.getOptionalStrictMappingKV,
      titleBf.getOptionalStrictMappingKV,
      descrBf.getOptionalStrictMappingKV
    )
    {(saleMaskColor, discountOpt, titleOpt, descrOpt) =>
      val blk = AOBlock(
        n = 0,
        discount = discountOpt,
        text1 = titleOpt,
        text2 = descrOpt
      )
      val bd = BlockDataImpl(
        blockMeta = getBlockMeta,
        offers = List(blk),
        colors = Map(saleMaskColorBf.name -> saleMaskColor)
      )
      BlockMapperResult(bd, bim = Map.empty)
    }
    {bmr =>
      val offerOpt = bmr.bd.offers.headOption
      val title = offerOpt.flatMap(_.text1)
      val descr = offerOpt.flatMap(_.text2)
      val discount = offerOpt.flatMap(_.discount)
      val saleMaskColor = bmr.unapplyColor(saleMaskColorBf)
      Some( (saleMaskColor, discount, title, descr) )
    }

    /** Шаблон для рендера. */
    override def template = _block12Tpl
  }


  val Block13 = new Val(13, "svgBgSlogan13") with SaveBgImg with Height {
    override def heightAvailableVals: Set[Int] = Set(300, 460)
    val discoIconColorBf = BfColor("discoIconColor", defaultValue = Some("828fa0"))
    val discoBorderColorBf = BfColor("discoBorderColor", defaultValue = Some("FFFFFF"))
    val discountBf = BfDiscount("discount", min = -99F, max = 999F)
    val textBf = BfText("descr", BlocksEditorFields.TextArea, maxLen = 256)

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    override def blockFields: List[BlockFieldT] = List(
      heightBf, discoIconColorBf, discoBorderColorBf, bgImgBf, discountBf, textBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = mapping(
      heightBf.getStrictMappingKV,
      discoIconColorBf.getStrictMappingKV,
      discoBorderColorBf.getStrictMappingKV,
      bgImgBf.getStrictMappingKV,
      discountBf.getOptionalStrictMappingKV,
      textBf.getOptionalStrictMappingKV
    )
    {(height, saleColor, saleMaskColor, bgBim, discountOpt, textOpt) =>
      val blk = AOBlock(
        n = 0,
        discount = discountOpt,
        text2 = textOpt
      )
      val bd = BlockDataImpl(
        blockMeta = BlockMeta(
          height = height,
          blockId = id
        ),
        offers = List(blk),
        colors = Map(
          discoIconColorBf.name -> saleColor,
          discoBorderColorBf.name -> saleMaskColor
        )
      )
      BlockMapperResult(bd, bgBim)
    }
    {bmr =>
      val height = bmr.bd.blockMeta.height
      val bgBim = bmr.unapplyBIM(bgImgBf)
      val offerOpt = bmr.bd.offers.headOption
      val discount = offerOpt.flatMap(_.discount)
      val text = offerOpt.flatMap(_.text2)
      val saleColor = bmr.unapplyColor(discoIconColorBf)
      val saleMaskColor = bmr.unapplyColor(discoBorderColorBf)
      Some( (height, saleColor, saleMaskColor, bgBim, discount, text) )
    }

    /** Шаблон для рендера. */
    override def template = _block13Tpl
  }


  sealed abstract class CommonBlock145(id: Int, name: String) extends Val(id, name) with SaveLogoImg with Height {
    override def heightAvailableVals: Set[Int] = Set(300, 460)
    val topColorBf = BfColor("topColor", defaultValue = Some("000000"))
    val bottomColorBf = BfColor("bottomColor", defaultValue = Some("bf6a6a"))
    val titleBf = BfText("title", BlocksEditorFields.TextArea, maxLen = 256)
    val descrBf = BfText("descr", BlocksEditorFields.TextArea, maxLen = 256)
    val lineColorBf = BfColor("lineColor", defaultValue = Some("B35151"))

    val blockWidth: Int

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    override def blockFields: List[BlockFieldT] = List(
      topColorBf, logoImgBf, bottomColorBf, lineColorBf, titleBf, descrBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = mapping(
      heightBf.getStrictMappingKV,
      topColorBf.getStrictMappingKV,
      logoImgBf.getStrictMappingKV,
      bottomColorBf.getStrictMappingKV,
      lineColorBf.getStrictMappingKV,
      titleBf.getOptionalStrictMappingKV,
      descrBf.getOptionalStrictMappingKV
    )
    {(height, topColor, logoBim, bottomColor, lineColor, titleOpt, descrOpt) =>
      val blk = AOBlock(
        n = 0,
        text1 = titleOpt,
        text2 = descrOpt
      )
      val bd = BlockDataImpl(
        blockMeta = BlockMeta(
          height = height,
          blockId = id
        ),
        offers = List(blk),
        colors = Map(
          topColorBf.name -> topColor,
          bottomColorBf.name -> bottomColor,
          lineColorBf.name -> lineColor
        )
      )
      BlockMapperResult(bd, logoBim)
    }
    {bmr =>
      val height = bmr.bd.blockMeta.height
      val logoBim = bmr.unapplyBIM(logoImgBf)
      val offerOpt = bmr.bd.offers.headOption
      val titleOpt = offerOpt.flatMap(_.text1)
      val descrOpt = offerOpt.flatMap(_.text2)
      val topColor = bmr.unapplyColor(topColorBf)
      val bottomColor = bmr.unapplyColor(bottomColorBf)
      val lineColor = bmr.unapplyColor(lineColorBf)
      Some( (height, topColor, logoBim, bottomColor, lineColor, titleOpt, descrOpt) )
    }
  }

  val Block14 = new CommonBlock145(14, "svgPictTitleDescr14") {
    override def template = _block14Tpl
    override val blockWidth: Int = 300
  }

  val Block15 = new CommonBlock145(15, "svgPictTitleDescrNarrow15") {
    override def template = _block15Tpl
    override val blockWidth: Int = 140
  }


  val Block16 = new Val(16, "titleDescPriceNopict") with HeightStatic {
    val titleBf = BfText("title", BlocksEditorFields.TextArea, maxLen = 256, withFontSizes = Set(65, 55, 45, 35, 28))
    val descrBf = BfText("descr", BlocksEditorFields.TextArea, maxLen = 256, withFontSizes = Set(36, 28, 22))
    val priceBf = BfPrice("price", withFontSizes = Set(65, 55, 45))
    val bgColorBf = BfColor("bgColor", defaultValue = Some("e1cea1"))
    val borderColorBf = BfColor("borderColor", defaultValue = Some("FFFFFF"))

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    override def blockFields: List[BlockFieldT] = List(
      heightBf, bgColorBf, borderColorBf, titleBf, descrBf, priceBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = mapping(
      heightBf.getStrictMappingKV,
      bgColorBf.getStrictMappingKV,
      borderColorBf.getStrictMappingKV,
      titleBf.getOptionalStrictMappingKV,
      descrBf.getOptionalStrictMappingKV,
      priceBf.getOptionalStrictMappingKV
    )
    {(height, bgColor, borderColor, titleOpt, descrOpt, priceOpt) =>
      val blk = AOBlock(
        n = 0,
        text1 = titleOpt,
        text2 = descrOpt,
        price = priceOpt
      )
      val bd = BlockDataImpl(
        blockMeta = BlockMeta(
          height = height,
          blockId = id
        ),
        offers = List(blk),
        colors = Map(
          bgColorBf.name      -> bgColor,
          borderColorBf.name  -> borderColor
        )
      )
      val bim: BlockImgMap = Map.empty
      BlockMapperResult(bd, bim)
    }
    {bmr =>
      val height = bmr.bd.blockMeta.height
      val offerOpt = bmr.bd.offers.headOption
      val titleOpt = offerOpt.flatMap(_.text1)
      val descrOpt = offerOpt.flatMap(_.text2)
      val bgColor = bmr.unapplyColor(bgColorBf)
      val borderColor = bmr.unapplyColor(borderColorBf)
      val priceOpt = offerOpt.flatMap(_.price)
      Some( (height, bgColor, borderColor, titleOpt, descrOpt, priceOpt) )
    }

    override def template = _block16Tpl
  }



  sealed abstract class CommonBlock17_18(id: Int, blkName: String) extends Val(id, blkName) with SaveBgImg with HeightStatic {
    val titleBf = BfText("title", BlocksEditorFields.TextArea, maxLen = 256)
    val discoBf = BfDiscount("discount",
      min = -99F,
      max = 99F,
      defaultValue = Some(AOFloatField(50F, defaultFont))
    )

    val bgColorBf = BfColor("bgColor", defaultValue = Some("FFFFFF"))
    val circleFillColorBf = BfColor("circleFillColor", defaultValue = Some("f9daac"))
    val discoIconColorBf = BfColor("discoIconColor", defaultValue = Some("ce2222"))
    val discoBorderColorBf = BfColor("discoBorderColor", defaultValue = Some("FFFFFF"))

    val blockWidth: Int

    override def blockFields: List[BlockFieldT] = List(
      heightBf, bgColorBf, bgImgBf, circleFillColorBf, titleBf, discoBf, discoIconColorBf, discoBorderColorBf
    )

    override def strictMapping: Mapping[BlockMapperResult] = mapping(
      heightBf.getStrictMappingKV,
      bgColorBf.getStrictMappingKV,
      bgImgBf.getStrictMappingKV,
      circleFillColorBf.getStrictMappingKV,
      titleBf.getOptionalStrictMappingKV,
      discoBf.getOptionalStrictMappingKV,
      discoIconColorBf.getStrictMappingKV,
      discoBorderColorBf.getStrictMappingKV
    )
    {(height, bgColor, bgBim, circleFillColor, titleOpt, discoOpt, saleIconColor, saleIconMaskColor) =>
      val blk = AOBlock(
        n = 0,
        text1 = titleOpt,
        discount = discoOpt
      )
      val bd = BlockDataImpl(
        blockMeta = BlockMeta(
          height = height,
          blockId = id
        ),
        offers = List(blk),
        colors = Map(
          bgColorBf.name           -> bgColor,
          circleFillColorBf.name   -> circleFillColor,
          discoIconColorBf.name     -> saleIconColor,
          discoBorderColorBf.name -> saleIconMaskColor
        )
      )
      BlockMapperResult(bd, bgBim)
    }
    {bmr =>
      val height = bmr.bd.blockMeta.height
      val offerOpt = bmr.bd.offers.headOption
      val titleOpt = offerOpt.flatMap(_.text1)
      val discoOpt = offerOpt.flatMap(_.discount)
      val bgColor = bmr.unapplyColor(bgColorBf)
      val circleFillColor = bmr.unapplyColor(circleFillColorBf)
      val saleIconColor = bmr.unapplyColor(discoIconColorBf)
      val saleIconMaskColor = bmr.unapplyColor(discoBorderColorBf)
      val bgBim = bmr.unapplyBIM(bgImgBf)
      Some( (height, bgColor, bgBim, circleFillColor, titleOpt, discoOpt, saleIconColor, saleIconMaskColor) )
    }
  }

  val Block17 = new CommonBlock17_18(17, "circlesAndDisco17") {
    override val blockWidth: Int = 300
    override def template = _block17Tpl
  }

  val Block18 = new CommonBlock17_18(18, "circlesAndDiscoNarrow18") {
    override val blockWidth: Int = 140
    override def template = _block18Tpl
  }

  
  val Block19 = new Val(19, "2prices19") with TitlePriceListBlockT with SaveBgImg with HeightStatic {
    override val offersCount = 2
    val bgColorBf = BfColor("bgColor", defaultValue = Some("000000"))
    val borderColorBf = BfColor("borderColor", defaultValue = Some("444444"))
    val fillColorBf = BfColor("fillColor", defaultValue = Some("666666"))

    /** Генерация описания полей. У нас тут повторяющийся маппинг, поэтому blockFields для редактора генерится без полей-констант. */
    override def blockFields: List[BlockFieldT] = {
      heightBf :: borderColorBf :: bgImgBf  :: bgColorBf :: fillColorBf :: super.blockFields
    }

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = {
      // Маппинг для всего блока.
      mapping(
        bgImgBf.getStrictMappingKV,
        heightBf.getStrictMappingKV,
        bgColorBf.getStrictMappingKV,
        borderColorBf.getStrictMappingKV,
        fillColorBf.getStrictMappingKV,
        "offer" -> offersMapping
      )
      {(bim, height, bgColor, borderColor, fillColor, offers) =>
        val bd: BlockData = BlockDataImpl(
          blockMeta = BlockMeta(
            height = height,
            blockId = id
          ),
          offers = offers,
          colors = Map(
            bgColorBf.name      -> bgColor,
            borderColorBf.name  -> borderColor,
            fillColorBf.name    -> fillColor
          )
        )
        BlockMapperResult(bd, bim)
      }
      {bmr =>
        val height = bmr.bd.blockMeta.height
        val bgColor     = bmr.unapplyColor(bgColorBf)
        val borderColor = bmr.unapplyColor(borderColorBf)
        val fillColor   = bmr.unapplyColor(fillColorBf)
        Some((bmr.bim, height, bgColor, borderColor, fillColor, bmr.bd.offers))
      }
    }

    /** Шаблон для рендера. */
    override def template = _block19Tpl
  }


  val Block20 = new Val(20, "block20") with SaveBgImg with HeightStatic {
    val titleBf = BfText("title", BlocksEditorFields.TextArea, maxLen = 256)
    val descrBf = BfText("descr", BlocksEditorFields.TextArea, maxLen = 256)

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    override def blockFields = List(
      bgImgBf, titleBf, descrBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = {
      mapping(
        heightBf.getStrictMappingKV,
        bgImgBf.getStrictMappingKV,
        titleBf.getOptionalStrictMappingKV,
        descrBf.getOptionalStrictMappingKV
      )
      {(height, bgBim, titleOpt, descrOpt) =>
        val blk = AOBlock(
          n = 0,
          text1 = titleOpt,
          text2 = descrOpt
        )
        val bd = BlockDataImpl(
          blockMeta = BlockMeta(
            height = height,
            blockId = id
          ),
          offers = List(blk)
        )
        BlockMapperResult(bd, bgBim)
      }
      {bmr =>
        val height = bmr.bd.blockMeta.height
        val bgBim = bmr.unapplyBIM(bgImgBf)
        val offerOpt = bmr.bd.offers.headOption
        val titleOpt = offerOpt.flatMap(_.text1)
        val descrOpt = offerOpt.flatMap(_.text2)
        Some( (height, bgBim, titleOpt, descrOpt) )
      }
    }

    override def template = _block20Tpl
  }


  val Block21 = new Val(21, "block20") with SaveBgImg with HeightStatic {
    val titleBf = BfText("title", BlocksEditorFields.TextArea, maxLen = 256)
    val descrBf = BfText("descr", BlocksEditorFields.TextArea, maxLen = 256)
    val borderColorBf = BfColor("borderColor", defaultValue = Some("95FF00"))

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    override def blockFields = List(
      bgImgBf, borderColorBf, titleBf, descrBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = {
      mapping(
        heightBf.getStrictMappingKV,
        bgImgBf.getStrictMappingKV,
        borderColorBf.getStrictMappingKV,
        titleBf.getOptionalStrictMappingKV,
        descrBf.getOptionalStrictMappingKV
      )
      {(height, bgBim, borderColor, titleOpt, descrOpt) =>
        val blk = AOBlock(
          n = 0,
          text1 = titleOpt,
          text2 = descrOpt
        )
        val bd = BlockDataImpl(
          blockMeta = BlockMeta(
            height = height,
            blockId = id
          ),
          offers = List(blk),
          colors = Map(borderColorBf.name -> borderColor)
        )
        BlockMapperResult(bd, bgBim)
      }
      {bmr =>
        val height = bmr.bd.blockMeta.height
        val bgBim = bmr.unapplyBIM(bgImgBf)
        val offerOpt = bmr.bd.offers.headOption
        val titleOpt = offerOpt.flatMap(_.text1)
        val descrOpt = offerOpt.flatMap(_.text2)
        val borderColor = bmr.unapplyColor(borderColorBf)
        Some( (height, bgBim, borderColor, titleOpt, descrOpt) )
      }
    }

    override def template = _block21Tpl
  }


  val Block22 = new Val(22, "block22") with SaveBgImg with SaveLogoImg with HeightStatic {
    val titleBf = BfText("title", BlocksEditorFields.TextArea, maxLen = 256)
    val descrBf = BfText("descr", BlocksEditorFields.TextArea, maxLen = 256)
    val borderColorBf = BfColor("borderColor", defaultValue = Some("FFFFFF"))

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    override def blockFields = List(
      logoImgBf, borderColorBf, bgImgBf, titleBf, descrBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = {
      mapping(
        heightBf.getStrictMappingKV,
        bgImgBf.getStrictMappingKV,
        borderColorBf.getStrictMappingKV,
        logoImgBf.getStrictMappingKV,
        titleBf.getOptionalStrictMappingKV,
        descrBf.getOptionalStrictMappingKV
      )
      {(height, bgBim, borderColor, logoBim, titleOpt, descrOpt) =>
        val blk = AOBlock(
          n = 0,
          text1 = titleOpt,
          text2 = descrOpt
        )
        val bd = BlockDataImpl(
          blockMeta = BlockMeta(
            height = height,
            blockId = id
          ),
          offers = List(blk),
          colors = Map(borderColorBf.name -> borderColor)
        )
        val bim = bgBim ++ logoBim
        BlockMapperResult(bd, bim)
      }
      {bmr =>
        val height = bmr.bd.blockMeta.height
        val bgBim = bmr.unapplyBIM(bgImgBf)
        val borderColor = bmr.unapplyColor(borderColorBf)
        val logoBim = bmr.unapplyBIM(logoImgBf)
        val offerOpt = bmr.bd.offers.headOption
        val titleOpt = offerOpt.flatMap(_.text1)
        val descrOpt = offerOpt.flatMap(_.text2)
        Some( (height, bgBim, borderColor, logoBim, titleOpt, descrOpt) )
      }
    }

    override def template = _block22Tpl
  }


  val Block23 = new Val(23, "somethng23") with SaveBgImg with HeightStatic {
    val fillColorBf = BfColor("fillColor", defaultValue = Some("f3f3f3"))
    val titleBf = BfText("title", BlocksEditorFields.TextArea, maxLen = 256)
    val descrBf = BfText("descr", BlocksEditorFields.TextArea, maxLen = 256)
    val priceBf = BfPrice("price")

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    override def blockFields: List[BlockFieldT] = List(
      heightBf, bgImgBf, titleBf, descrBf, priceBf
    )

    /** Набор маппингов для обработки данных от формы. */
    def strictMapping: Mapping[BlockMapperResult] = {
      mapping(
        heightBf.getStrictMappingKV,
        bgImgBf.getStrictMappingKV,
        fillColorBf.getStrictMappingKV,
        titleBf.getOptionalStrictMappingKV,
        descrBf.getOptionalStrictMappingKV,
        priceBf.getOptionalStrictMappingKV
      )
      {(height, bgBim, fillColor, titleOpt, descrOpt, priceOpt) =>
        val blk = AOBlock(
          n = 0,
          text1 = titleOpt,
          text2 = descrOpt,
          price = priceOpt
        )
        val bd = BlockDataImpl(
          blockMeta = BlockMeta(
            height = height,
            blockId = id
          ),
          offers = List(blk),
          colors = Map(fillColorBf.name -> fillColor)
        )
        BlockMapperResult(bd, bgBim)
      }
      {bmr =>
        val height = bmr.bd.blockMeta.height
        val bgBim = bmr.unapplyBIM(bgImgBf)
        val fillColor = bmr.unapplyColor(fillColorBf)
        val offerOpt = bmr.bd.offers.headOption
        val titleOpt = offerOpt.flatMap(_.text1)
        val descrOpt = offerOpt.flatMap(_.text2)
        val priceOpt = offerOpt.flatMap(_.price)
        Some( (height, bgBim, fillColor, titleOpt, descrOpt, priceOpt) )
      }
    }

    def template = _block23Tpl
  }


  val Block24 = new Val(24, "smth24") with SaveBgImg with SaveLogoImg with HeightStatic {
    val fillColorBf = BfColor("fillColor", defaultValue = Some("d5c864"))
    val titleBf = BfText("title", BlocksEditorFields.TextArea, maxLen = 256)
    val priceBf = BfPrice("price")
    val oldPriceBf = BfPrice("oldPrice")

    override def blockFields: List[BlockFieldT] = List(
      heightBf, logoImgBf, bgImgBf, fillColorBf, titleBf, priceBf, oldPriceBf
    )

    def strictMapping: Mapping[BlockMapperResult] = {
      mapping(
        heightBf.getStrictMappingKV,
        logoImgBf.getStrictMappingKV,
        bgImgBf.getStrictMappingKV,
        fillColorBf.getStrictMappingKV,
        titleBf.getOptionalStrictMappingKV,
        priceBf.getOptionalStrictMappingKV,
        oldPriceBf.getOptionalStrictMappingKV
      )
      {(height, logoBim, bgBim, fillColor, titleOpt, priceOpt, oldPriceOpt) =>
        val blk = AOBlock(
          n = 0,
          text1 = titleOpt,
          price = priceOpt,
          oldPrice = oldPriceOpt
        )
        val bd = BlockDataImpl(
          blockMeta = BlockMeta(
            height = height,
            blockId = id
          ),
          offers = List(blk),
          colors = Map(fillColorBf.name -> fillColor)
        )
        val bim = bgBim ++ logoBim
        BlockMapperResult(bd, bim)
      }
      {bmr =>
        val height = bmr.bd.blockMeta.height
        val logoBim = bmr.unapplyBIM(logoImgBf)
        val bgBim = bmr.unapplyBIM(bgImgBf)
        val fillColor = bmr.unapplyColor(fillColorBf)
        val offerOpt = bmr.bd.offers.headOption
        val titleOpt = offerOpt.flatMap(_.text1)
        val priceOpt = offerOpt.flatMap(_.price)
        val oldPriceOpt = offerOpt.flatMap(_.oldPrice)
        Some( (height, logoBim, bgBim, fillColor, titleOpt, priceOpt, oldPriceOpt) )
      }
    }

    def template = _block24Tpl
  }


  /** Сортированные значения. Обращение напрямую к values порождает множество с неопределённым порядком,
    * а тут - сразу отсортировано по id. */
  val valuesSorted = values.toSeq.sortBy(_.id)
}


case class BlockMapperResult(bd: BlockData, bim: BlockImgMap) {
  def unapplyColor(bfc: BfColor): String = bd.colors.getOrElse(bfc.name, bfc.anyDefaultValue)
  def unapplyBIM(bfi: BfImage): BlockImgMap = bim.filter(_._1 == bfi.name)
}


object SaveImgUtil {

  def saveImgsStatic(newImgs: BlockImgMap, oldImgs: Imgs_t, supImgsFut: Future[Imgs_t], fn: String): Future[Imgs_t] = {
    val needImgsThis = newImgs.get(fn)
    val oldImgsThis = oldImgs.get(fn)
    // Нанооптимизация: не ворочить картинками, если нет по ним никакой инфы.
    if (needImgsThis.isDefined || oldImgsThis.isDefined) {
      // Есть картинки для обработки (старые или новые), запустить обработку.
      val saveBgImgFut = ImgFormUtil.updateOrigImg(needImgs = needImgsThis,  oldImgs = oldImgsThis)
      for {
        savedBgImg <- saveBgImgFut
        supSavedMap <- supImgsFut
      } yield {
        savedBgImg
          .fold(supSavedMap) {
          savedBgImg => supSavedMap + (fn -> savedBgImg)
        }
      }
    } else {
      // Нет данных по картинкам. Можно спокойно возвращать исходный фьючерс.
      supImgsFut
    }
  }

}


/** Интерфейс для сохранения картинок. */
sealed trait ISaveImgs {
  def saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t): Future[Imgs_t] = {
    Future successful Map.empty
  }
}


/** Функционал для сохранения фоновой (основной) картинки блока. */
sealed trait SaveBgImg extends ISaveImgs {
  // Константы можно легко переопределить т.к. trait и early initializers.
  val BG_IMG_FN = "bgImg"
  val bgImgBf = BfImage(BG_IMG_FN, marker = BG_IMG_FN, imgUtil = OrigImageUtil)

  override def saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t): Future[Imgs_t] = {
    val supImgsFut = super.saveImgs(newImgs, oldImgs)
    SaveImgUtil.saveImgsStatic(
      newImgs = newImgs,
      oldImgs = oldImgs,
      supImgsFut = supImgsFut,
      fn = BG_IMG_FN
    )
  }

}


/** Функционал для сохранения вторичного логотипа рекламной карточки. */
sealed trait SaveLogoImg extends ISaveImgs {
  val LOGO_IMG_FN = "logo"
  val logoImgBf = BfImage(LOGO_IMG_FN, marker = LOGO_IMG_FN, imgUtil = AdnLogoImageUtil)  // Запилить отдельный конвертор для логотипов на карточках?

  override def saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t): Future[Imgs_t] = {
    val supImgsFut = super.saveImgs(newImgs, oldImgs)
    SaveImgUtil.saveImgsStatic(
      newImgs = newImgs,
      oldImgs = oldImgs,
      supImgsFut = supImgsFut,
      fn = LOGO_IMG_FN
    )
  }
}


/** Базовый интерфейс для реализаций класса Enumeration.Val. */
sealed trait ValT extends ISaveImgs {
  def id: Int

  /** Шаблон для рендера. */
  def template: Template3[MAdT, Boolean, Context, HtmlFormat.Appendable]

  /** Набор маппингов для обработки данных от формы. */
  def strictMapping: Mapping[BlockMapperResult]

  /** Более удобный интерфейс для метода template.render(). */
  def renderBlock(mad: MAdT, isStandalone: Boolean)(implicit ctx: Context) = {
    template.render(mad, isStandalone, ctx)
  }

  /**
   * label'ы опций конфига блока, прописанные в conf/messages*.
   * @param bk исходный BK_-идентификатор
   * @return идентификатор, пригодный для резолва через Messages().
   */
  def i18nLabelOf(bk: String): String

 
  /** Stackable-trait заполняется в прямом порядке в отличии от списка [[blockFields]].
    * Этот метод помогает заполнять список полей задом наперёд. */
  def blockFieldsRev: List[BlockFieldT]
  
  /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
  def blockFields: List[BlockFieldT] = blockFieldsRev.reverse
 
  def blockFieldForName(n: String): Option[BlockFieldT] = {
    blockFields.find(_.name equalsIgnoreCase n)
  }

  def getBlockMeta(height: Int) = BlockMeta(blockId = id, height = height)

  /** Отрендерить редактор. */
  def renderEditor(af: Form[_], formDataSer: Option[String])(implicit ctx: util.Context): HtmlFormat.Appendable
}



/** Для сборки блоков, обрабатывающие блоки с офферами вида "title+price много раз", используется этот трейт. */
trait TitlePriceListBlockT {
  // Названия используемых полей.
  val TITLE_FN = "title"
  val PRICE_FN = "price"

  /** Начало отсчета счетчика офферов. */
  val N0 = 0

  /** Макс кол-во офферов (макс.длина списка офферов). */
  def offersCount: Int

  protected def bfText(offerNopt: Option[Int]) = BfText(TITLE_FN, BlocksEditorFields.TextArea, maxLen = 128, offerNopt = offerNopt)
  protected def bfPrice(offerNopt: Option[Int]) = BfPrice(PRICE_FN, offerNopt = offerNopt)

  /** Генерация описания полей. У нас тут повторяющийся маппинг, поэтому blockFields для редактора генерится без полей-констант. */
  def blockFields: List[BlockFieldT] = {
    (N0 until offersCount)
      .flatMap { offerN =>
      val offerNopt = Some(offerN)
      val titleBf = bfText(offerNopt)
      val priceBf = bfPrice(offerNopt)
      List(titleBf, priceBf)
    }
      .toList
  }

  // Поля оффера
  protected def titleMapping = bfText(None)
  protected def priceMapping = bfPrice(None)

  // Маппинг для одного элемента (оффера)
  protected def offerMapping = tuple(
    titleMapping.getOptionalStrictMappingKV,
    priceMapping.getOptionalStrictMappingKV
  )
  // Маппинг для списка офферов.
  protected def offersMapping = list(offerMapping)
    .verifying("error.too.much", { _.size <= offersCount })
    .transform[List[AOBlock]] (applyAOBlocks, unapplyAOBlocks)


  /** Собрать AOBlock на основе куска выхлопа формы. */
  protected def applyAOBlocks(l: List[(Option[AOStringField], Option[AOPriceField])]): List[AOBlock] = {
    l.iterator
      // Делаем zipWithIndex перед фильтром чтобы сохранять выравнивание на странице (css-классы), если 1 или 2 элемент пропущен.
      .zipWithIndex
      // Выкинуть пустые офферы
      .filter {
      case ((titleOpt, priceOpt), _) =>
        titleOpt.isDefined || priceOpt.isDefined
    }
      // Оставшиеся офферы завернуть в AOBlock
      .map {
      case ((titleOpt, priceOpt), i) =>
        AOBlock(n = i,  text1 = titleOpt,  price = priceOpt)
    }
      .toList
  }

  /** unapply для offersMapping. Вынесен для упрощения кода. Метод восстанавливает исходный выхлоп формы,
    * даже если были пропущены какие-то группы полей. */
  protected def unapplyAOBlocks(aoBlocks: Seq[AOBlock]) = {
    // без if isEmpty будет экзепшен в maxBy().
    if (aoBlocks.isEmpty) {
      Nil
    } else {
      // Вычисляем оптимальную длину списка результатов
      val maxN = aoBlocks.maxBy(_.n).n
      // Рисуем карту маппингов необходимой длины, ключ - это n.
      val aoBlocksNS = aoBlocks
        .map { aoBlock => aoBlock.n -> aoBlock }
        .toMap
      // Восстанавливаем новый список выхлопов мапперов на основе длины и имеющихся экземпляров AOBlock.
      (N0 to maxN)
        .map { n =>
        aoBlocksNS
          .get(n)
          .map { aoBlock => aoBlock.text1 -> aoBlock.price }
          .getOrElse(None -> None)
      }
        .toList
    }
  }
}



object BlocksConfUtilHeight {
  val BLOCK_HEIGHT_DFLT_VALUE = Some(300)
  val AVAILABLE_VALS_DFLT = Set(300, 460, 620)
  val BF_HEIGHT_DFLT = BfHeight(
    name = BlockMeta.HEIGHT_ESFN,
    defaultValue = BLOCK_HEIGHT_DFLT_VALUE,
    availableVals = AVAILABLE_VALS_DFLT
  )
}

trait HeightT extends ValT {
  def heightBf: BfHeight
  def heightDefaultValue: Option[Int]
  def heightAvailableVals: Set[Int]

  /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
  abstract override def blockFieldsRev: List[BlockFieldT] = heightBf :: super.blockFields
}

trait HeightStatic extends HeightT {
  import BlocksConfUtilHeight._
  // final - для защиты от ошибочной перезаписи полей. При наступлении необходимости надо заюзать Height вместо HeightStatic
  override final def heightBf = BF_HEIGHT_DFLT
  override final def heightDefaultValue = BLOCK_HEIGHT_DFLT_VALUE
  override final def heightAvailableVals = AVAILABLE_VALS_DFLT
}

trait Height extends HeightT {
  import BlocksConfUtilHeight._
  override def heightDefaultValue = BLOCK_HEIGHT_DFLT_VALUE
  override def heightAvailableVals = AVAILABLE_VALS_DFLT
  override def heightBf = BfHeight(
    name = BlockMeta.HEIGHT_ESFN,
    defaultValue = heightDefaultValue,
    availableVals = heightAvailableVals
  )
}

trait Height300 extends ValT {
  val HEIGHT = 300
  def getBlockMeta: BlockMeta = getBlockMeta(HEIGHT)
}

