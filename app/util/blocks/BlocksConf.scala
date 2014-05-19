package util.blocks

import play.api.templates._
import play.api.data._, Forms._
import BlocksUtil._
import views.html.blocks._
import models._
import io.suggest.ym.model.common.BlockMeta
import util.blocks.BlocksUtil.BlockImgMap
import scala.Some
import play.api.data.validation.Constraint
import util.img.{ImgIdKey, ImgInfo4Save}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.14 16:50
 * Description: Конфиги для блоков верстки.
 */

object BlocksConf extends Enumeration {

  /** Всё описание блока идёт через наследование Val и её интерфейса [[ValT]] при необходимости. */
  protected abstract class Val(id: Int) extends super.Val(id, "Block" + id) with ValTEmpty {

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
  sealed trait Block1t extends HeightStatic with BgImg with TitleStatic with OldPrice with Price {
    override def priceDefaultValue = Some(AOPriceField(100F, "RUB", "100 р.", defaultFont))
    override def oldPriceDefaultValue = Some(AOPriceField(200F, "RUB", "200 р.", defaultFont))
    override def template = _block1Tpl
  }
  val Block1 = new Val(1) with Block1t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block1Wrapper(key = newKey)
  }
  sealed case class Block1Wrapper(key: String) extends ValTWrapper(Block1) with ValTEmpty with Block1t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  /** Блок картинки с двумя текстами. */
  sealed trait Block2t extends BgImg with Height with TitleStatic with Descr {
    override def heightAvailableVals: Set[Int] = Set(300, 460)
    override def descrDefaultValue: Option[AOStringField] = {
      Some(AOStringField("Распродажа. Сегодня. Сейчас.", AOFieldFont("000000")))
    }
    override def template = _block2Tpl
  }
  val Block2 = new Val(2) with Block2t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block2Wrapper(key = newKey)
  }
  sealed case class Block2Wrapper(key: String) extends ValTWrapper(Block2) with ValTEmpty with Block1t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  /** Блок с тремя ценами в первом дизайне. */
  sealed trait Block3t extends BgImg with HeightStatic with TitlePriceListBlockT {
    override val offersCount = 3
    override def template = _block3Tpl
  }
  val Block3 = new Val(3) with Block3t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block3Wrapper(key = newKey)
  }
  sealed case class Block3Wrapper(key: String) extends ValTWrapper(Block3) with ValTEmpty with Block3t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  sealed trait CommonBlock4_9 extends ValT with BgImg with HeightFixed
  with TitleStatic with PriceStatic with DescrStatic with BgColor with BorderColor {
    override def bgColorDefaultValue: Option[String] = Some("0F2841")
    override def borderColorDefaultValue: Option[String] = Some("FFFFFF")
    def blockWidth: Int
  }


  /** Рекламный блок с предложением товара/услуги и рекламным посылом. */
  sealed trait Block4t extends CommonBlock4_9 {
    override val blockWidth = 300
    override def template = _block4Tpl
  }
  val Block4 = new Val(4) with Block4t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block4Wrapper(key = newKey)
  }
  sealed case class Block4Wrapper(key: String) extends ValTWrapper(Block4) with ValTEmpty with Block4t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  /** Реклама брендированного товара. От предыдущих одно-офферных блоков отличается дизайном и тем, что есть вторичный логотип. */
  sealed trait Block5t extends BgImg with HeightStatic with MaskColor with LogoImg with TitleStatic
    with PriceStatic with OldPriceStatic {
    override def maskColorDefaultValue: Option[String] = Some("d5c864")
    override def template = _block5Tpl
  }
  val Block5 = new Val(5) with Block5t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block5Wrapper(key = newKey)
  }
  sealed case class Block5Wrapper(key: String) extends ValTWrapper(Block5) with ValTEmpty with Block5t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  /** Блок, который содержит до трёх офферов с ценами. Аналог [[Block3]], но с иным дизайном. */
  sealed trait Block6t extends BgImg with TitlePriceListBlockT with HeightFixed {
    override val offersCount = 3
    override def template = _block6Tpl
  }
  val Block6 = new Val(6) with Block6t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block6Wrapper(key = newKey)
  }
  sealed case class Block6Wrapper(key: String) extends ValTWrapper(Block6) with ValTEmpty with Block6t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  /** Блок, отображающий скидочную цену на товар или услугу. */
  sealed case class Block7c(key: String = "") extends Val(7) with HeightFixed with SaleMaskColor with DiscountStatic
  with TitleStatic with PriceStatic {
    // 2014.may.06: Цвета для слова SALE и фона рамки с %показателем скидки.
    override def saleMaskColorDefaultValue: Option[String] = Some("00ff1a")
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
    override def template = _block7Tpl
  }
  val Block7 = Block7c()


  sealed case class Block8c(key: String = "") extends Val(8) with BgImg with TitleStatic with PriceStatic with HeightFixed {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
    override def template = _block8Tpl
  }
  val Block8 = Block8c()
  

  sealed case class Block9c(key: String = "") extends Val(9) with CommonBlock4_9 {
    override val blockWidth = 140
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
    override def template = _block9Tpl
  }
  val Block9 = Block9c()


  sealed case class Block10c(key: String = "") extends Val(10) with BgImg with TitleStatic with OldPriceStatic
  with PriceStatic with HeightFixed {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
    override def template = _block10Tpl
  }
  val Block10 = Block10c()


  sealed case class Block11c(key: String = "") extends Val(11) with SaleMaskColor with BgImg with HeightFixed
  with TitleStatic with DescrStatic {
    override def saleMaskColorDefaultValue: Option[String] = Some("aaaaaa")
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
    override def template = _block11Tpl
  }
  val Block11 = Block11c()


  sealed case class Block12c(key: String = "") extends Val(12) with HeightFixed with SaleMaskColor with DiscountStatic
  with TitleStatic with DescrStatic {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
    override def saleMaskColorDefaultValue: Option[String] = Some("00ff1a")
    override def template = _block12Tpl
  }
  val Block12 = Block12c()


  sealed case class Block13c(key: String = "") extends Val(13) with Height with DiscoIconColor
  with DiscoBorderColorStatic with BgImg with DiscountStatic with DescrStatic {
    override def heightAvailableVals: Set[Int] = Set(300, 460)
    override def discoIconColorDefaultValue: Option[String] = Some("828fa0")
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
    override def template = _block13Tpl
  }
  val Block13 = Block13c()


  sealed trait CommonBlock145 extends TopColor with LogoImg with BottomColor with HeightI with LineColor
  with TitleStatic with DescrStatic {
    override def topColorDefaultValue: Option[String] = Some("000000")
    override def bottomColorDefaultValue: Option[String] = Some("bf6a6a")
    override def lineColorDefaultValue: Option[String] = Some("B35151")
    val blockWidth: Int
  }

  sealed case class Block14c(key: String = "") extends Val(14) with CommonBlock145 with HeightPlain {
    override def template = _block14Tpl
    override val blockWidth: Int = 300
    override def heightAvailableVals = Set(300, 460)
    override def blockFields: List[BlockFieldT] = heightBf :: super.blockFields
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }
  val Block14 = Block14c()

  sealed case class Block15c(key: String = "") extends Val(15) with CommonBlock145 with HeightPlain {
    override def template = _block15Tpl
    override val blockWidth: Int = 140
    override def heightAvailableVals = Set(300)
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }
  val Block15 = Block15c()


  sealed case class Block16c(key: String = "") extends Val(16) with HeightStatic with BgColor with BorderColor
  with Title with Descr with Price {
    override def titleFontSizes: Set[Int] = Set(65, 55, 45, 35, 28)
    override def descrFontSizes: Set[Int] = Set(36, 28, 22)
    override def priceFontSizes: Set[Int] = Set(65, 55, 45)
    override def borderColorDefaultValue: Option[String] = Some("FFFFFF")
    override def bgColorDefaultValue: Option[String] = Some("e1cea1")

    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
    override def template = _block16Tpl
  }
  val Block16 = Block16c()


  sealed trait CommonBlock17_18 extends BgColor with SaveBgImgI with CircleFillColor with HeightI with TitleStatic
  with DiscountStatic with DiscoIconColor with DiscoBorderColorStatic {
    override def bgColorDefaultValue: Option[String] = Some("FFFFFF")
    override def discoIconColorDefaultValue: Option[String] = Some("ce2222")
    override def circleFillColorDefaultValue: Option[String] = Some("f9daac")

    val blockWidth: Int
  }

  sealed case class Block17c(key: String = "") extends Val(17) with CommonBlock17_18 with HeightPlain with BgImg {
    override val blockWidth: Int = 300
    override def heightAvailableVals: Set[Int] = Set(300, 460)
    // Добавляем в начало формы поле высоты.
    override def blockFields: List[BlockFieldT] = heightBf :: super.blockFields
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
    override def template = _block17Tpl
  }
  val Block17 = Block17c()

  sealed case class Block18c(key: String = "") extends Val(18) with CommonBlock17_18 with HeightPlain with BgImg {
    override val blockWidth: Int = 140
    override def heightAvailableVals: Set[Int] = Set(300)
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
    override def template = _block18Tpl
  }
  val Block18 = Block18c()


  sealed case class Block19c(key: String = "") extends Val(19) with HeightStatic with BgImg with BorderColor
  with TitlePriceListBlockT with BgColor with FillColor {
    override val offersCount = 2
    override def borderColorDefaultValue: Option[String] = Some("444444")
    override def bgColorDefaultValue: Option[String] = Some("000000")
    override def fillColorDefaultValue: Option[String] = Some("666666")

    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
    override def template = _block19Tpl
  }
  val Block19 = Block19c()


  sealed case class Block20c(key: String = "") extends Val(20) with HeightStatic with BgImg with TitleStatic
  with DescrStatic {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
    override def template = _block20Tpl
  }
  val Block20 = Block20c()


  sealed case class Block21c(key: String = "") extends Val(21) with HeightStatic with BgImg with BorderColor
  with TitleStatic with DescrStatic {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
    override def borderColorDefaultValue: Option[String] = Some("95FF00")
    override def template = _block21Tpl
  }
  val Block21 = Block21c()


  sealed case class Block22c(key: String = "") extends Val(22) with HeightStatic with LogoImg with BorderColor
  with BgImg with TitleStatic with DescrStatic {
    override def borderColorDefaultValue: Option[String] = Some("FFFFFF")
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
    override def template = _block22Tpl
  }
  val Block22 = Block22c()


  sealed case class Block23c(key: String = "") extends Val(23) with BgImg with TitleStatic with DescrStatic
  with PriceStatic with FillColor with HeightFixed {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
    override def fillColorDefaultValue: Option[String] = Some("f3f3f3")
    override def template = _block23Tpl
  }
  val Block23 = Block23c()


  sealed case class Block24c(key: String = "") extends Val(24) with LogoImg with BgImg with FillColor with HeightStatic
  with TitleStatic with PriceStatic with OldPriceStatic {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
    override def fillColorDefaultValue: Option[String] = Some("d5c864")
    override def template = _block24Tpl
  }
  val Block24 = Block24c()


  /** Сортированные значения. Обращение напрямую к values порождает множество с неопределённым порядком,
    * а тут - сразу отсортировано по id. */
  val valuesSorted = values.toSeq.sortBy(_.id)
}


case class BlockMapperResult(bd: BlockData, bim: BlockImgMap) {
  def unapplyColor(bfc: BfColor): String = bd.colors.getOrElse(bfc.name, bfc.anyDefaultValue)
  def unapplyBIM(bfi: BfImage): BlockImgMap = bim.filter(_._1 == bfi.name)
  def flatMapFirstOffer[T](f: AOBlock => Option[T]) = bd.offers.headOption.flatMap(f)
}


/** Базовый интерфейс для реализаций класса Enumeration.Val. */
trait ValT extends ISaveImgs with Mapping[BlockMapperResult] {
  def id: Int

  /** Шаблон для рендера. */
  def template: Template3[MAdT, Boolean, Context, HtmlFormat.Appendable]

  /** Набор маппингов для обработки данных от формы. */
  def strictMapping: Mapping[BlockMapperResult] = this

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

  // Mapping:
  def mappingsAcc: List[Mapping[_]]
  override val mappings = mappingsAcc

  override val constraints: Seq[Constraint[BlockMapperResult]] = Nil
  override def verifying(constraints: Constraint[BlockMapperResult]*): Mapping[BlockMapperResult] = ???

  def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc]
  override def bind(data: Map[String, String]): Either[Seq[FormError], BlockMapperResult] = {
    // Собрать BindAcc и сконвиртить в BlockMapperResult
    bindAcc(data).right.map { bindAcc =>
      val blockMeta = BlockMeta(blockId = id, height = bindAcc.height)
      val bd = BlockDataImpl(blockMeta,
        offers = bindAcc.offers,
        colors = bindAcc.colors.toMap
      )
      BlockMapperResult(bd, bindAcc.bim.toMap)
    }
  }

  def mappingWithNewKey(newKey: String): Mapping[BlockMapperResult]
  def withPrefix(prefix: String) = addPrefix(prefix).map(mappingWithNewKey).getOrElse(this)
}


case class BindAcc(
  var colors: List[(String, String)] = Nil,
  var offers: List[AOBlock] = Nil,
  var height: Int = 300,
  var bim: List[(String, ImgInfo4Save[ImgIdKey])] = Nil
)


abstract class ValTWrapper(v: ValT) extends ValT {
  override def id = v.id
  override def i18nLabelOf(bk: String) = v.i18nLabelOf(bk)
  override def renderEditor(af: Form[_], formDataSer: Option[String])(implicit ctx: Context): HtmlFormat.Appendable = {
    v.renderEditor(af, formDataSer)
  }
}


/** Враппер понадобился из-за проблем со scala.Enumeration, который не даёт делать инстансы Val несколько раз. */
trait ValTEmpty extends ValT {
  override def blockFieldsRev: List[BlockFieldT] = Nil
  override def unbind(value: BlockMapperResult): Map[String, String] = {
    Map.empty
  }
  override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    Map.empty[String, String] -> Seq.empty[FormError]
  }
  override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = Right(BindAcc())
  override def mappingsAcc: List[Mapping[_]] = Nil
}


trait EmptyKey extends ValT {
  override val key: String = ""
}

