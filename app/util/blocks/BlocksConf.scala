package util.blocks

import play.api.templates._
import play.api.data._, Forms._
import BlocksUtil._
import util.FormUtil._
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

  /** Интерфейс для сохранения картинок. */
  protected trait ISaveImgs {
    def saveImgs(newImgs: BlockImgMap, oldImgs: Imgs_t): Future[Imgs_t] = {
      Future successful Map.empty
    }
  }


  /** Функционал для сохранения фоновой (основной) картинки блока. */
  protected trait SaveBgImg extends ISaveImgs {
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
  protected trait SaveLogoImg extends ISaveImgs {
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


  /** Всё описание блока идёт через наследование Val. */
  protected abstract class Val(id: Int, name: String) extends super.Val(id, name) with ISaveImgs {
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
    def i18nLabelOf(bk: String) = I18N_PREFIX + bk

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    def blockFields: List[BlockFieldT]

    def blockFieldForName(n: String): Option[BlockFieldT] = {
      blockFields.find(_.name equalsIgnoreCase n)
    }

    /** Отрендерить редактор. */
    def renderEditor(af: Form[_], formDataSer: Option[String])(implicit ctx: util.Context): HtmlFormat.Appendable = {
      editor._blockEditorTpl(af, withBC = Some(this), formDataSer = formDataSer)
    }
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
  val Block1 = new Val(1, "photoAdnPrice") with SaveBgImg {

    val heightField = BfHeight(BlockMeta.HEIGHT_ESFN, defaultValue = Some(300), availableVals = Set(300, 460, 620))
    val text1Field = BfText("title", BlocksEditorFields.InputText,
      minLen = 0,
      maxLen = 64,
      defaultValue = Some(AOStringField("Платье", AOFieldFont("444444")))
    )
    val oldPriceField = BfPrice(EMAdOffers.OLD_PRICE_ESFN,
      defaultValue = Some(AOPriceField(200F, "RUB", "200 р.", defaultFont))
    )
    val priceField = BfPrice(EMAdOffers.PRICE_ESFN,
      defaultValue = Some(AOPriceField(100F, "RUB", "100 р.", defaultFont))
    )

    override def blockFields = List(
      bgImgBf, heightField, text1Field, oldPriceField, priceField
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping = mapping(
      bgImgBf.getStrictMappingKV,
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
    val heightField = BfHeight(BlockMeta.HEIGHT_ESFN, defaultValue = Some(300), availableVals = Set(300, 460))
    val text1Field = BfText(EMAdOffers.TEXT1_ESFN, BlocksEditorFields.InputText, maxLen = 512)
    val text2Field = BfText(EMAdOffers.TEXT2_ESFN, BlocksEditorFields.TextArea, maxLen = 8192,
      defaultValue = Some(AOStringField("Распродажа. Сегодня. Сейчас.", AOFieldFont("000000")))
    )

    override def blockFields = List(
      bgImgBf, heightField, text1Field, text2Field
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping = mapping(
      bgImgBf.getStrictMappingKV,
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


  /** Карточка с картинкой и списком title+price. Такой блок встречается несколько раз с разным дизайном. */
  sealed abstract class TitlePriceListBlock(id: Int, name: String) extends Val(id, name) with SaveBgImg {
    // Названия используемых полей.
    val TITLE_FN = "title"
    val PRICE_FN = "price"

    /** Начало отсчета счетчика офферов. */
    val N0 = 0
    /** Макс кол-во офферов (макс.длина списка офферов). */
    val OFFERS_COUNT = 3

    def heightAvailableVals: Set[Int]

    val heightField = BfHeight(BlockMeta.HEIGHT_ESFN, defaultValue = Some(300), availableVals = heightAvailableVals)

    protected def bfText(offerNopt: Option[Int]) = BfText(TITLE_FN, BlocksEditorFields.TextArea, maxLen = 128, offerNopt = offerNopt)
    protected def bfPrice(offerNopt: Option[Int]) = BfPrice(PRICE_FN, offerNopt = offerNopt)

    /** Генерация описания полей. У нас тут повторяющийся маппинг, поэтому blockFields для редактора генерится без полей-констант. */
    override def blockFields: List[BlockFieldT] = {
      val fns = (N0 until OFFERS_COUNT)
        .flatMap { offerN =>
          val offerNopt = Some(offerN)
          val titleBf = bfText(offerNopt)
          val priceBf = bfPrice(offerNopt)
          List(titleBf, priceBf)
        }
        .toList
      bgImgBf :: heightField :: fns
    }

    /** Маппинг для обработки сабмита формы блока. */
    override def strictMapping: Mapping[BlockMapperResult] = {
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
        .verifying("error.too.much", { _.size <= OFFERS_COUNT })
        .transform[List[AOBlock]] (applyAOBlocks, unapplyAOBlocks)
      // Маппинг для всего блока.
      mapping(
        bgImgBf.getStrictMappingKV,
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


  /** Блок с тремя ценами в первом дизайне. */
  val Block3 = new TitlePriceListBlock(3, "3prices") {

    def heightAvailableVals = Set(300, 460, 620)

    /** Шаблон для рендера. */
    override def template = _block3Tpl
  }


  /** Рекламный блок с предложением товара/услуги и рекламным посылом. */
  val Block4 = new Val(4, "2texts+price") with SaveBgImg {
    val heightBf = BfHeight(BlockMeta.HEIGHT_ESFN, defaultValue = Some(300), availableVals = Set(300))
    val text1bf = BfText("text1", BlocksEditorFields.InputText, maxLen = 256)
    val priceBf = BfPrice("price")
    val text2bf = BfText("text2", BlocksEditorFields.TextArea, maxLen = 512)
    val bgColorBf = BfColor("bgColor", defaultValue = Some("0F2841"))

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    override def blockFields: List[BlockFieldT] = List(
      bgImgBf, text1bf, priceBf, text2bf, bgColorBf
    )

    /** Маппинг для обработки данных от сабмита формы блока. */
    override def strictMapping: Mapping[BlockMapperResult] = {
      mapping(
        bgImgBf.getStrictMappingKV,
        heightBf.getStrictMappingKV,
        text1bf.getOptionalStrictMappingKV,
        priceBf.getOptionalStrictMappingKV,
        text2bf.getOptionalStrictMappingKV,
        bgColorBf.getStrictMappingKV
      )
      {(bim, height, text1Opt, priceOpt, text2Opt, bgColor) =>
        val blk = AOBlock(
          n = 0,
          text1 = text1Opt,
          price = priceOpt,
          text2 = text2Opt
        )
        val bd: BlockData = BlockDataImpl(
          blockMeta = BlockMeta(
            height = height,
            blockId = id
          ),
          offers = List(blk),
          colors = Map(bgColorBf.name -> bgColor)
        )
        BlockMapperResult(bd, bim)
      }
      {bmr =>
        val height = bmr.bd.blockMeta.height
        val offerOpt = bmr.bd.offers.headOption
        val text1 = offerOpt.flatMap(_.text1)
        val price = offerOpt.flatMap(_.price)
        val text2 = offerOpt.flatMap(_.text2)
        val bgColor = bmr.bd.colors.get(bgColorBf.name).getOrElse(bgColorBf.anyDefaultValue)
        Some( (bmr.bim, height, text1, price, text2, bgColor) )
      }
    }

    /** Шаблон для рендера. */
    override def template = _block4Tpl
  }


  /** Реклама брендированного товара. От предыдущих одно-офферных блоков отличается дизайном и тем, что есть вторичный логотип. */
  val Block5 = new Val(5, "brandedProduct") with SaveBgImg with SaveLogoImg {
    val heightBf = BfHeight(BlockMeta.HEIGHT_ESFN, defaultValue = Some(300), availableVals = Set(300, 460))
    val text1Bf = BfText("title", BlocksEditorFields.TextArea, maxLen = 256)
    val oldPriceBf = BfPrice("oldPrice")
    val priceBf = BfPrice("price")

    override def blockFields: List[BlockFieldT] = List(
      bgImgBf, heightBf, logoImgBf, text1Bf, oldPriceBf, priceBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = mapping(
      bgImgBf.getStrictMappingKV,
      logoImgBf.getOptionalStrictMappingKV,
      heightBf.getStrictMappingKV,
      text1Bf.getOptionalStrictMappingKV,
      oldPriceBf.getOptionalStrictMappingKV,
      priceBf.getOptionalStrictMappingKV
    )
    {(bgBim, logoBim, height, text1Opt, oldPriceOpt, priceOpt) =>
      val block = AOBlock(
        n = 0,
        text1 = text1Opt,
        oldPrice = oldPriceOpt,
        price = priceOpt
      )
      val bd: BlockData = BlockDataImpl(
        blockMeta = BlockMeta(
          height = height,
          blockId = id
        ),
        offers = List(block)
      )
      val bim: BlockImgMap = logoBim.fold(bgBim) { bgBim ++ _ }
      BlockMapperResult(bd, bim)
    }
    {bmr =>
      val bgBim: BlockImgMap = bmr.bim.filter(_._1 == bgImgBf.name)
      val logoBim: BlockImgMap = bmr.bim.filter(_._1 == logoImgBf.name)
      val logoBimOpt = if (logoBim.isEmpty) None else Some(logoBim)
      val height = bmr.bd.blockMeta.height
      val offerOpt = bmr.bd.offers.headOption
      val text1 = offerOpt.flatMap(_.text1)
      val oldPrice = offerOpt.flatMap(_.oldPrice)
      val price = offerOpt.flatMap(_.price)
      Some( (bgBim, logoBimOpt, height, text1, oldPrice, price) )
    }

    /** Шаблон для рендера. */
    override def template = _block5Tpl
  }


  /** Блок, который содержит до трёх офферов с ценами. Аналог [[Block3]], но с иным дизайном. */
  val Block6 = new TitlePriceListBlock(6, "3prices2") {

    def heightAvailableVals = Set(300)

    override def blockFields: List[BlockFieldT] = {
      val fns = (N0 until OFFERS_COUNT)
        .flatMap { offerN =>
        val offerNopt = Some(offerN)
        val titleBf = bfText(offerNopt)
        val priceBf = bfPrice(offerNopt)
        List(titleBf, priceBf)
      }
        .toList
      bgImgBf :: fns
    }

    /** Шаблон для рендера. */
    override def template = _block6Tpl
  }


  /** Блок, отображающий скидочную цену на товар или услугу. */
  val Block7 = new Val(7, "discountedPrice1") {
    val heightBf = BfHeight(BlockMeta.HEIGHT_ESFN, defaultValue = Some(300), availableVals = Set(300))
    val discoBf = BfDiscount("discount", min = -9.9F, max = 99F)
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
        heightBf.getStrictMappingKV,
        saleMaskColorBf.getStrictMappingKV,
        iconBgColorBf.getStrictMappingKV,
        discoBf.getOptionalStrictMappingKV,
        titleBf.getOptionalStrictMappingKV,
        priceBf.getOptionalStrictMappingKV
      )
      {(height, bannerFontColor, iconBgColor, discoOpt, titleOpt, priceOpt) =>
        val blk = AOBlock(
          n = 0,
          text1 = titleOpt,
          discount = discoOpt,
          price = priceOpt
        )
        val bd: BlockData = BlockDataImpl(
          blockMeta = BlockMeta(
            height = height,
            blockId = id
          ),
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
        val height = bmr.bd.blockMeta.height
        val offerOpt = bmr.bd.offers.headOption
        val discount = offerOpt.flatMap(_.discount)
        val title = offerOpt.flatMap(_.text1)
        val price = offerOpt.flatMap(_.price)
        val saleMaskColor = bmr.bd.colors.get(saleMaskColorBf.name).getOrElse(saleMaskColorBf.anyDefaultValue)
        val iconBgColor = bmr.bd.colors.get(iconBgColorBf.name).getOrElse(iconBgColorBf.anyDefaultValue)
        Some( (height, saleMaskColor, iconBgColor, discount, title, price) )
      }
    }

    /** Шаблон для рендера. */
    override def template = _block7Tpl
  }


  val Block8 = new Val(8, "titleWithPrice8") with SaveBgImg {
    val heightBf = BfHeight(BlockMeta.HEIGHT_ESFN, defaultValue = Some(300), availableVals = Set(300))
    val titleBf = BfText("title", BlocksEditorFields.TextArea, maxLen = 256)
    val priceBf = BfPrice("price")

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    override def blockFields: List[BlockFieldT] = List(
      bgImgBf, titleBf, priceBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = mapping(
      bgImgBf.getStrictMappingKV,
      heightBf.getStrictMappingKV,
      titleBf.getOptionalStrictMappingKV,
      priceBf.getOptionalStrictMappingKV
    )
    // apply()
    {(bim, height, titleOpt, priceOpt) =>
      val blk = AOBlock(
        n = 0,
        text1 = titleOpt,
        price = priceOpt
      )
      val bd: BlockData = BlockDataImpl(
        blockMeta = BlockMeta(
          height = height,
          blockId = id
        ),
        offers = List(blk)
      )
      BlockMapperResult(bd, bim)
    }
    // unapply()
    {bmr =>
      val height = bmr.bd.blockMeta.height
      val bgBim: BlockImgMap = bmr.bim.filter(_._1 == bgImgBf.name)
      val offerOpt = bmr.bd.offers.headOption
      val title = offerOpt.flatMap(_.text1)
      val price = offerOpt.flatMap(_.price)
      Some( (bgBim, height, title, price) )
    }

    /** Шаблон для рендера. */
    override def template = _block8Tpl
  }
  
  
  val Block9 = new Val(9, "titlePriceDescrNarrow9") with SaveBgImg {
    val heightBf = BfHeight(BlockMeta.HEIGHT_ESFN, defaultValue = Some(300), availableVals = Set(300))
    val titleBf = BfText("title", BlocksEditorFields.TextArea, maxLen = 256)
    val priceBf = BfPrice("price")
    val descrBf = BfText("descr", BlocksEditorFields.TextArea, maxLen = 256)

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    override def blockFields: List[BlockFieldT] = List(
      bgImgBf, titleBf, priceBf, descrBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = mapping(
      heightBf.getStrictMappingKV,
      bgImgBf.getStrictMappingKV,
      titleBf.getOptionalStrictMappingKV,
      priceBf.getOptionalStrictMappingKV,
      descrBf.getOptionalStrictMappingKV
    )
    // apply()
    {(height, bgBim, titleOpt, priceOpt, descrOpt) =>
      val blk = AOBlock(
        n = 0,
        text1 = titleOpt,
        price = priceOpt,
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
      val bgBim: BlockImgMap = bmr.bim.filter(_._1 == bgImgBf.name)
      val offerOpt = bmr.bd.offers.headOption
      val title = offerOpt.flatMap(_.text1)
      val price = offerOpt.flatMap(_.price)
      val descr = offerOpt.flatMap(_.text2)
      Some( (height, bgBim, title, price, descr) )
    }

    /** Шаблон для рендера. */
    override def template = _block9Tpl
  }


  val Block10 = new Val(10, "oldNewPriceNarrow10") with SaveBgImg {
    val heightBf    = BfHeight(BlockMeta.HEIGHT_ESFN, defaultValue = Some(300), availableVals = Set(300))
    val titleBf     = BfText("title", BlocksEditorFields.TextArea, maxLen = 256)
    val oldPriceBf  = BfPrice("oldPrice")
    val priceBf     = BfPrice("price")

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    override def blockFields: List[BlockFieldT] = List(
      bgImgBf, titleBf, oldPriceBf, priceBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = mapping(
      heightBf.getStrictMappingKV,
      bgImgBf.getStrictMappingKV,
      titleBf.getOptionalStrictMappingKV,
      oldPriceBf.getOptionalStrictMappingKV,
      priceBf.getOptionalStrictMappingKV
    )
    {(height, bgBim, titleOpt, oldPriceOpt, priceOpt) =>
      val blk = AOBlock(
        n = 0,
        text1 = titleOpt,
        oldPrice = oldPriceOpt,
        price = priceOpt
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
      val bgBim: BlockImgMap = bmr.bim.filter(_._1 == bgImgBf.name)
      val offerOpt = bmr.bd.offers.headOption
      val title = offerOpt.flatMap(_.text1)
      val oldPrice = offerOpt.flatMap(_.oldPrice)
      val price = offerOpt.flatMap(_.price)
      Some( (height, bgBim, title, oldPrice, price) )
    }

    /** Шаблон для рендера. */
    override def template = _block10Tpl
  }


  val Block11 = new Val(11, "promoNarrow11") with SaveBgImg {
    val heightBf = BfHeight(BlockMeta.HEIGHT_ESFN, defaultValue = Some(300), availableVals = Set(300))
    val titleBf = BfText("title", BlocksEditorFields.TextArea, maxLen = 256)
    val descrBf = BfText("descr", BlocksEditorFields.TextArea, maxLen = 256)
    val saleMaskColorBf = BfColor("saleMaskColor", defaultValue = Some("aaaaaa"))

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    override def blockFields: List[BlockFieldT] = List(
      saleMaskColorBf, bgImgBf, titleBf, descrBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = mapping(
      heightBf.getStrictMappingKV,
      saleMaskColorBf.getStrictMappingKV,
      bgImgBf.getStrictMappingKV,
      titleBf.getOptionalStrictMappingKV,
      descrBf.getOptionalStrictMappingKV
    )
    {(height, saleMaskColor, bgBim, titleOpt, descrOpt) =>
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
        colors = Map(saleMaskColorBf.name -> saleMaskColor)
      )
      BlockMapperResult(bd, bgBim)
    }
    {bmr =>
      val height = bmr.bd.blockMeta.height
      val bgBim: BlockImgMap = bmr.bim.filter(_._1 == bgImgBf.name)
      val offerOpt = bmr.bd.offers.headOption
      val title = offerOpt.flatMap(_.text1)
      val descr = offerOpt.flatMap(_.text2)
      val saleMaskColor = bmr.bd.colors.get(saleMaskColorBf.name).getOrElse(saleMaskColorBf.anyDefaultValue)
      Some( (height, saleMaskColor, bgBim, title, descr) )
    }

    /** Шаблон для рендера. */
    override def template = _block11Tpl
  }


  val Block12 = new Val(12, "discountNarrow12") {
    val heightBf = BfHeight(BlockMeta.HEIGHT_ESFN, defaultValue = Some(300), availableVals = Set(300))
    val saleMaskColorBf = BfColor("saleMaskColor", defaultValue = Some("00ff1a"))
    val discountBf = BfDiscount("discount", min = -9.9F, max = 99F)
    val titleBf = BfText("title", BlocksEditorFields.TextArea, maxLen = 256)
    val descrBf = BfText("descr", BlocksEditorFields.TextArea, maxLen = 256)

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    override def blockFields: List[BlockFieldT] = List(
      saleMaskColorBf, discountBf, titleBf, descrBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = mapping(
      heightBf.getStrictMappingKV,
      saleMaskColorBf.getStrictMappingKV,
      discountBf.getOptionalStrictMappingKV,
      titleBf.getOptionalStrictMappingKV,
      descrBf.getOptionalStrictMappingKV
    )
    {(height, saleMaskColor, discountOpt, titleOpt, descrOpt) =>
      val blk = AOBlock(
        n = 0,
        discount = discountOpt,
        text1 = titleOpt,
        text2 = descrOpt
      )
      val bd = BlockDataImpl(
        blockMeta = BlockMeta(
          height = height,
          blockId = id
        ),
        offers = List(blk),
        colors = Map(saleMaskColorBf.name -> saleMaskColor)
      )
      BlockMapperResult(bd, bim = Map.empty)
    }
    {bmr =>
      val height = bmr.bd.blockMeta.height
      val offerOpt = bmr.bd.offers.headOption
      val title = offerOpt.flatMap(_.text1)
      val descr = offerOpt.flatMap(_.text2)
      val discount = offerOpt.flatMap(_.discount)
      val saleMaskColor = bmr.bd.colors.get(saleMaskColorBf.name).getOrElse(saleMaskColorBf.anyDefaultValue)
      Some( (height, saleMaskColor, discount, title, descr) )
    }

    /** Шаблон для рендера. */
    override def template = _block12Tpl
  }


  val Block13 = new Val(13, "svgBgSlogan13") with SaveBgImg {
    val heightBf = BfHeight(BlockMeta.HEIGHT_ESFN, defaultValue = Some(300), availableVals = Set(300, 460))
    val saleColorBf = BfColor("saleColor", defaultValue = Some("828fa0"))
    val saleMaskColorBf = BfColor("saleMaskColor", defaultValue = Some("FFFFFF"))
    val discountBf = BfDiscount("discount", min = -9.9F, max = 99F)
    val textBf = BfText("descr", BlocksEditorFields.TextArea, maxLen = 256)

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    override def blockFields: List[BlockFieldT] = List(
      heightBf, saleColorBf, saleMaskColorBf, bgImgBf, discountBf, textBf
    )

    /** Набор маппингов для обработки данных от формы. */
    override def strictMapping: Mapping[BlockMapperResult] = mapping(
      heightBf.getStrictMappingKV,
      saleColorBf.getStrictMappingKV,
      saleMaskColorBf.getStrictMappingKV,
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
          saleColorBf.name -> saleColor,
          saleMaskColorBf.name -> saleMaskColor
        )
      )
      BlockMapperResult(bd, bgBim)
    }
    {bmr =>
      val height = bmr.bd.blockMeta.height
      val bgBim: BlockImgMap = bmr.bim.filter(_._1 == bgImgBf.name)
      val offerOpt = bmr.bd.offers.headOption
      val discount = offerOpt.flatMap(_.discount)
      val text = offerOpt.flatMap(_.text2)
      val saleColor = bmr.bd.colors.get(saleColorBf.name).getOrElse(saleColorBf.anyDefaultValue)
      val saleMaskColor = bmr.bd.colors.get(saleMaskColorBf.name).getOrElse(saleMaskColorBf.anyDefaultValue)
      Some( (height, saleColor, saleMaskColor, bgBim, discount, text) )
    }

    /** Шаблон для рендера. */
    override def template = _block13Tpl
  }


  sealed abstract class CommonBlock145(id: Int, name: String) extends Val(id, name) with SaveLogoImg {
    val heightBf = BfHeight(BlockMeta.HEIGHT_ESFN, defaultValue = Some(300), availableVals = Set(300, 460))
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
      val logoBim = bmr.bim.filter(_._1 == logoImgBf.name)
      val offerOpt = bmr.bd.offers.headOption
      val titleOpt = offerOpt.flatMap(_.text1)
      val descrOpt = offerOpt.flatMap(_.text2)
      val topColor = bmr.bd.colors.get(topColorBf.name).getOrElse(topColorBf.anyDefaultValue)
      val bottomColor = bmr.bd.colors.get(bottomColorBf.name).getOrElse(bottomColorBf.anyDefaultValue)
      val lineColor = bmr.bd.colors.get(lineColorBf.name).getOrElse(lineColorBf.anyDefaultValue)
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


  val Block16 = new Val(16, "titleDescPriceNopict") {
    val heightBf = BfHeight(BlockMeta.HEIGHT_ESFN, defaultValue = Some(300), availableVals = Set(300, 460, 620))
    val titleBf = BfText("title", BlocksEditorFields.TextArea, maxLen = 256)
    val descrBf = BfText("descr", BlocksEditorFields.TextArea, maxLen = 256)
    val priceBf = BfPrice("price")
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
      val bgColor = bmr.bd.colors.get(bgColorBf.name).getOrElse(bgColorBf.anyDefaultValue)
      val borderColor = bmr.bd.colors.get(borderColorBf.name).getOrElse(borderColorBf.anyDefaultValue)
      val priceOpt = offerOpt.flatMap(_.price)
      Some( (height, bgColor, borderColor, titleOpt, descrOpt, priceOpt) )
    }

    override def template = _block16Tpl
  }


  sealed abstract class CommonBlock17_18(id: Int, blkName: String) extends Val(id, blkName) with SaveBgImg {
    val heightBf = BfHeight(BlockMeta.HEIGHT_ESFN, defaultValue = Some(300), availableVals = Set(300, 460, 620))
    val titleBf = BfText("title", BlocksEditorFields.TextArea, maxLen = 256)
    val discoBf = BfDiscount("discount", min = -9.9F, max = 99F)

    val bgColorBf = BfColor("bgColor", defaultValue = Some("FFFFFF"))
    val circleFillColorBf = BfColor("circleFillColor", defaultValue = Some("f9daac"))
    val saleIconColorBf = BfColor("saleIconColor", defaultValue = Some("ce2222"))
    val saleIconMaskColorBf = BfColor("saleIconMaskColor", defaultValue = Some("FFFFFF"))

    val blockWidth: Int

    override def blockFields: List[BlockFieldT] = List(
      heightBf, bgColorBf, bgImgBf, circleFillColorBf, titleBf, discoBf, saleIconColorBf, saleIconMaskColorBf
    )

    override def strictMapping: Mapping[BlockMapperResult] = mapping(
      heightBf.getStrictMappingKV,
      bgColorBf.getStrictMappingKV,
      bgImgBf.getStrictMappingKV,
      circleFillColorBf.getStrictMappingKV,
      titleBf.getOptionalStrictMappingKV,
      discoBf.getOptionalStrictMappingKV,
      saleIconColorBf.getStrictMappingKV,
      saleIconMaskColorBf.getStrictMappingKV
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
          saleIconColorBf.name     -> saleIconColor,
          saleIconMaskColorBf.name -> saleIconMaskColor
        )
      )
      BlockMapperResult(bd, bgBim)
    }
    {bmr =>
      val height = bmr.bd.blockMeta.height
      val offerOpt = bmr.bd.offers.headOption
      val titleOpt = offerOpt.flatMap(_.text1)
      val discoOpt = offerOpt.flatMap(_.discount)
      val bgColor = bmr.bd.colors.get(bgColorBf.name).getOrElse(bgColorBf.anyDefaultValue)
      val circleFillColor = bmr.bd.colors.getOrElse(circleFillColorBf.name, circleFillColorBf.anyDefaultValue)
      val saleIconColor = bmr.bd.colors.getOrElse(saleIconColorBf.name, saleIconColorBf.anyDefaultValue)
      val saleIconMaskColor = bmr.bd.colors.getOrElse(saleIconMaskColorBf.name, saleIconMaskColorBf.anyDefaultValue)
      val bgBim: BlockImgMap = bmr.bim.filter(_._1 == bgImgBf.name)
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


  /** Сортированные значения. Обращение напрямую к values порождает множество с неопределённым порядком,
    * а тут - сразу отсортировано по id. */
  val valuesSorted = values.toSeq.sortBy(_.id)
}


case class BlockMapperResult(bd: BlockData, bim: BlockImgMap)


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

