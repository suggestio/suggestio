package util.blocks

import play.api.templates._
import play.api.data._, Forms._
import BlocksUtil._
import views.html.blocks._
import models._
import io.suggest.ym.model.common.BlockMeta
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
  val Block1 = new Val(1, "photoAdnPrice") with HeightStatic with BgImg with TitleStatic with OldPrice with Price {
    override def priceDefaultValue = Some(AOPriceField(100F, "RUB", "100 р.", defaultFont))
    override def oldPriceDefaultValue = Some(AOPriceField(200F, "RUB", "200 р.", defaultFont))

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
  val Block2 = new Val(2, "saleWithText") with BgImg with Height with TitleStatic with Descr {
    override def heightAvailableVals: Set[Int] = Set(300, 460)
    override def descrDefaultValue: Option[AOStringField] = {
      Some(AOStringField("Распродажа. Сегодня. Сейчас.", AOFieldFont("000000")))
    }

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
  val Block3 = new Val(3, "3prices") with BgImg with HeightStatic with TitlePriceListBlockT {
    override val offersCount = 3

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


  sealed abstract class CommonBlock4_9(id: Int, name: String) extends Val(id, name) with BgImg with HeightFixed
  with TitleStatic with PriceStatic with DescrStatic with BgColor with BorderColor {
    override def bgColorDefaultValue: Option[String] = Some("0F2841")
    override def borderColorDefaultValue: Option[String] = Some("FFFFFF")

    val blockWidth: Int

    /** Маппинг для обработки данных от сабмита формы блока. */
    override def strictMapping: Mapping[BlockMapperResult] = {
      mapping(
        bgImgBf.getStrictMappingKV,
        titleBf.getOptionalStrictMappingKV,
        priceBf.getOptionalStrictMappingKV,
        descrBf.getOptionalStrictMappingKV,
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
  val Block5 = new Val(5, "brandedProduct5") with BgImg with HeightStatic with MaskColor with LogoImg with TitleStatic
    with PriceStatic with OldPriceStatic {
    override def maskColorDefaultValue: Option[String] = Some("d5c864")

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
  val Block6 = new Val(6, "3prices2") with BgImg with TitlePriceListBlockT with HeightFixed {
    override val offersCount = 3

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
  val Block7 = new Val(7, "discountedPrice1") with HeightFixed with SaleMaskColor with DiscountStatic with TitleStatic
  with PriceStatic {
    // 2014.may.06: Цвета для слова SALE и фона рамки с %показателем скидки.
    override def saleMaskColorDefaultValue: Option[String] = Some("00ff1a")

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = {
      mapping(
        saleMaskColorBf.getStrictMappingKV,
        discountBf.getOptionalStrictMappingKV,
        titleBf.getOptionalStrictMappingKV,
        priceBf.getOptionalStrictMappingKV
      )
      {(bannerFontColor, discoOpt, titleOpt, priceOpt) =>
        val blk = AOBlock(
          n = 0,
          text1 = titleOpt,
          discount = discoOpt,
          price = priceOpt
        )
        val bd: BlockData = BlockDataImpl(
          blockMeta = getBlockMeta,
          offers = List(blk),
          colors = Map(saleMaskColorBf.name -> bannerFontColor)
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
        Some( (saleMaskColor, discount, title, price) )
      }
    }

    /** Шаблон для рендера. */
    override def template = _block7Tpl
  }


  val Block8 = new Val(8, "titleWithPrice8") with BgImg with TitleStatic with PriceStatic with HeightFixed {
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


  val Block10 = new Val(10, "oldNewPriceNarrow10") with BgImg with TitleStatic with OldPriceStatic
  with PriceStatic with HeightFixed {
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


  val Block11 = new Val(11, "promoNarrow11") with SaleMaskColor with BgImg with HeightFixed with TitleStatic with DescrStatic {
    override def saleMaskColorDefaultValue: Option[String] = Some("aaaaaa")

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


  val Block12 = new Val(12, "discountNarrow12") with HeightFixed with SaleMaskColor with DiscountStatic with TitleStatic
  with DescrStatic {
    override def saleMaskColorDefaultValue: Option[String] = Some("00ff1a")

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


  val Block13 = new Val(13, "svgBgSlogan13") with Height with DiscoIconColor with DiscoBorderColorStatic with BgImg
  with DiscountStatic with DescrStatic {
    override def heightAvailableVals: Set[Int] = Set(300, 460)
    override def discoIconColorDefaultValue: Option[String] = Some("828fa0")

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = mapping(
      heightBf.getStrictMappingKV,
      discoIconColorBf.getStrictMappingKV,
      discoBorderColorBf.getStrictMappingKV,
      bgImgBf.getStrictMappingKV,
      discountBf.getOptionalStrictMappingKV,
      descrBf.getOptionalStrictMappingKV
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


  sealed abstract class CommonBlock145(id: Int, name: String) extends Val(id, name) with TopColor with LogoImg
  with BottomColor with HeightI with LineColor with TitleStatic with DescrStatic {
    override def topColorDefaultValue: Option[String] = Some("000000")
    override def bottomColorDefaultValue: Option[String] = Some("bf6a6a")
    override def lineColorDefaultValue: Option[String] = Some("B35151")

    val blockWidth: Int

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

  val Block14 = new CommonBlock145(14, "svgPictTitleDescr14") with HeightPlain {
    override def template = _block14Tpl
    override val blockWidth: Int = 300
    override def heightAvailableVals = Set(300, 460)
    override def blockFields: List[BlockFieldT] = heightBf :: super.blockFields
  }

  val Block15 = new CommonBlock145(15, "svgPictTitleDescrNarrow15") with HeightPlain {
    override def template = _block15Tpl
    override val blockWidth: Int = 140
    override def heightAvailableVals = Set(300)
  }


  val Block16 = new Val(16, "titleDescPriceNopict") with HeightStatic with BgColor with BorderColor with Title with Descr with Price {
    override def titleFontSizes: Set[Int] = Set(65, 55, 45, 35, 28)
    override def descrFontSizes: Set[Int] = Set(36, 28, 22)
    override def priceFontSizes: Set[Int] = Set(65, 55, 45)
    override def borderColorDefaultValue: Option[String] = Some("FFFFFF")
    override def bgColorDefaultValue: Option[String] = Some("e1cea1")

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



  sealed abstract class CommonBlock17_18(id: Int, blkName: String) extends Val(id, blkName) with BgColor
  with SaveBgImgI with CircleFillColor with HeightI with TitleStatic with DiscountStatic with DiscoIconColor
  with DiscoBorderColorStatic {
    override def bgColorDefaultValue: Option[String] = Some("FFFFFF")
    override def discoIconColorDefaultValue: Option[String] = Some("ce2222")
    override def circleFillColorDefaultValue: Option[String] = Some("f9daac")

    val blockWidth: Int

    override def strictMapping: Mapping[BlockMapperResult] = mapping(
      heightBf.getStrictMappingKV,
      bgColorBf.getStrictMappingKV,
      bgImgBf.getStrictMappingKV,
      circleFillColorBf.getStrictMappingKV,
      titleBf.getOptionalStrictMappingKV,
      discountBf.getOptionalStrictMappingKV,
      discoIconColorBf.getStrictMappingKV,
      discoBorderColorBf.getStrictMappingKV
    )
    {(height, bgColor, bgBim, circleFillColor, titleOpt, discoOpt, discoIconColor, discoBorderColor) =>
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
          discoIconColorBf.name    -> discoIconColor,
          discoBorderColorBf.name  -> discoBorderColor
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
      val discoIconColor = bmr.unapplyColor(discoIconColorBf)
      val discoIconMaskColor = bmr.unapplyColor(discoBorderColorBf)
      val bgBim = bmr.unapplyBIM(bgImgBf)
      Some( (height, bgColor, bgBim, circleFillColor, titleOpt, discoOpt, discoIconColor, discoIconMaskColor) )
    }
  }

  val Block17 = new CommonBlock17_18(17, "circlesAndDisco17") with HeightPlain with BgImg {
    override val blockWidth: Int = 300
    override def heightAvailableVals: Set[Int] = Set(300, 460)
    // Добавляем в начало формы поле высоты.
    override def blockFields: List[BlockFieldT] = heightBf :: super.blockFields
    override def template = _block17Tpl
  }

  val Block18 = new CommonBlock17_18(18, "circlesAndDiscoNarrow18") with HeightPlain with BgImg {
    override val blockWidth: Int = 140
    override def heightAvailableVals: Set[Int] = Set(300)
    override def template = _block18Tpl
  }

  
  val Block19 = new Val(19, "2prices19") with HeightStatic with BgImg with BorderColor with TitlePriceListBlockT
  with BgColor with FillColor {
    override val offersCount = 2
    override def borderColorDefaultValue: Option[String] = Some("444444")
    override def bgColorDefaultValue: Option[String] = Some("000000")
    override def fillColorDefaultValue: Option[String] = Some("666666")

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


  val Block20 = new Val(20, "block20") with HeightStatic with BgImg with TitleStatic with DescrStatic {
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


  val Block21 = new Val(21, "block20") with HeightStatic with BgImg with BorderColor with TitleStatic with DescrStatic {
    override def borderColorDefaultValue: Option[String] = Some("95FF00")

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


  val Block22 = new Val(22, "block22") with HeightStatic with LogoImg with BorderColor with BgImg
  with TitleStatic with DescrStatic {
    override def borderColorDefaultValue: Option[String] = Some("FFFFFF")

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


  val Block23 = new Val(23, "somethng23") with BgImg with TitleStatic with DescrStatic with PriceStatic
  with FillColor with HeightFixed {
    override def fillColorDefaultValue: Option[String] = Some("f3f3f3")

    /** Набор маппингов для обработки данных от формы. */
    def strictMapping: Mapping[BlockMapperResult] = {
      mapping(
        bgImgBf.getStrictMappingKV,
        fillColorBf.getStrictMappingKV,
        titleBf.getOptionalStrictMappingKV,
        descrBf.getOptionalStrictMappingKV,
        priceBf.getOptionalStrictMappingKV
      )
      {(bgBim, fillColor, titleOpt, descrOpt, priceOpt) =>
        val blk = AOBlock(
          n = 0,
          text1 = titleOpt,
          text2 = descrOpt,
          price = priceOpt
        )
        val bd = BlockDataImpl(
          blockMeta = getBlockMeta,
          offers = List(blk),
          colors = Map(fillColorBf.name -> fillColor)
        )
        BlockMapperResult(bd, bgBim)
      }
      {bmr =>
        val bgBim = bmr.unapplyBIM(bgImgBf)
        val fillColor = bmr.unapplyColor(fillColorBf)
        val offerOpt = bmr.bd.offers.headOption
        val titleOpt = offerOpt.flatMap(_.text1)
        val descrOpt = offerOpt.flatMap(_.text2)
        val priceOpt = offerOpt.flatMap(_.price)
        Some( (bgBim, fillColor, titleOpt, descrOpt, priceOpt) )
      }
    }

    def template = _block23Tpl
  }


  val Block24 = new Val(24, "smth24") with LogoImg with BgImg with FillColor with HeightStatic
  with TitleStatic with PriceStatic with OldPriceStatic {
    override def fillColorDefaultValue: Option[String] = Some("d5c864")

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


/** Базовый интерфейс для реализаций класса Enumeration.Val. */
trait ValT extends ISaveImgs {
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

