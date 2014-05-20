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
  sealed trait Block1t extends Height with BgImg with Title with OldPrice with Price {
    override def titleBf = super.titleBf.copy(
      defaultValue = Some(AOStringField("Платье", AOFieldFont("444444")))
    )
    override def priceBf = super.priceBf.copy(
      defaultValue = Some(AOPriceField(100F, "RUB", "100 р.", defaultFont))
    )
    override def oldPriceBf = super.oldPriceBf.copy(
      defaultValue = Some(AOPriceField(200F, "RUB", "200 р.", defaultFont))
    )
    override def template = _block1Tpl
  }
  val Block1 = new Val(1) with Block1t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block1Wrapper(key = newKey)
  }
  sealed case class Block1Wrapper(key: String) extends ValTWrapper(Block1) with ValTEmpty with Block1t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  /** Блок картинки с двумя текстами. */
  sealed trait Block2t extends BgImg with Height with Title with Descr {
    override def heightBf: BfHeight = super.heightBf.copy(
      availableVals = Set(300, 460)
    )
    override def descrBf = super.descrBf.copy(
      defaultValue = Some(AOStringField("Распродажа. Сегодня. Сейчас.", AOFieldFont("000000")))
    )
    override def template = _block2Tpl
  }
  val Block2 = new Val(2) with Block2t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block2Wrapper(key = newKey)
  }
  sealed case class Block2Wrapper(key: String) extends ValTWrapper(Block2) with ValTEmpty with Block2t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  /** Блок с тремя ценами в первом дизайне. */
  sealed trait Block3t extends Height with BgImg with TitlePriceListBlockT {
    override def offersCount = 3
    override def template = _block3Tpl
  }
  val Block3 = new Val(3) with EmptyKey with Block3t {
    override def mappingWithNewKey(newKey: String) = Block3Wrapper(key = newKey)
  }
  sealed case class Block3Wrapper(key: String) extends ValTWrapper(Block3) with ValTEmpty with Block3t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  sealed trait CommonBlock4_9 extends ValT with BgImg with HeightFixed
  with Title with Price with Descr with BgColor with BorderColor {
    override def bgColorBf: BfColor = super.bgColorBf.copy(
      defaultValue = Some("0F2841")
    )
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
  sealed trait Block5t extends BgImg with Height with MaskColor with LogoImg with Title with Price with OldPrice {
    override def maskColorBf: BfColor = super.maskColorBf.copy(
      defaultValue = Some("d5c864")
    )
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
  sealed trait Block7t extends HeightFixed with SaleMaskColor with Discount with Title with Price {
    override def template = _block7Tpl
    override def saleMaskColorBf: BfColor = super.saleMaskColorBf.copy(
      defaultValue = Some("00ff1a")
    )
  }
  val Block7 = new Val(7) with Block7t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block7Wrapper(key = newKey)
  }
  sealed case class Block7Wrapper(key: String) extends ValTWrapper(Block7) with ValTEmpty with Block7t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  sealed trait Block8t extends BgImg with Title with Price with HeightFixed {
    override def template = _block8Tpl
  }
  val Block8 = new Val(8) with Block8t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block8Wrapper(key = newKey)
  }
  sealed case class Block8Wrapper(key: String) extends ValTWrapper(Block8) with ValTEmpty with Block8t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  sealed trait Block9t extends CommonBlock4_9 {
    override val blockWidth = 140
    override def template = _block9Tpl
  }
  val Block9 = new Val(9) with Block9t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block9Wrapper(key = newKey)
  }
  sealed case class Block9Wrapper(key: String) extends ValTWrapper(Block9) with ValTEmpty with Block9t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  sealed trait Block10t extends BgImg with Title with OldPrice with Price with HeightFixed {
    override def template = _block10Tpl
  }
  val Block10 = new Val(10) with Block10t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block10Wrapper(key = newKey)
  }
  sealed case class Block10Wrapper(key: String) extends ValTWrapper(Block9) with ValTEmpty with Block10t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  sealed trait Block11t extends SaleMaskColor with BgImg with HeightFixed with Title with Descr {
    override def saleMaskColorBf: BfColor = super.saleMaskColorBf.copy(
      defaultValue = Some("AAAAAA")
    )
    override def template = _block11Tpl
  }
  val Block11 = new Val(11) with Block11t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block11Wrapper(key = newKey)
  }
  sealed case class Block11Wrapper(key: String) extends ValTWrapper(Block10) with ValTEmpty with Block11t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  sealed trait Block12t extends HeightFixed with SaleMaskColor with Discount with Title with Descr {
    override def saleMaskColorBf: BfColor = super.saleMaskColorBf.copy(
      defaultValue = Some("00ff1a")
    )
    override def template = _block12Tpl
  }
  val Block12 = new Val(12) with Block12t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block12Wrapper(key = newKey)
  }
  sealed case class Block12Wrapper(key: String) extends ValTWrapper(Block12) with ValTEmpty with Block12t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  sealed trait Block13t extends Height with DiscoIconColor with DiscoBorderColor with BgImg with Discount with Descr {
    override def heightBf: BfHeight = super.heightBf.copy(
      availableVals = Set(300, 460)
    )
    override def discoIconColorBf: BfColor = super.discoIconColorBf.copy(
      defaultValue = Some("828fa0")
    )
    override def template = _block13Tpl
  }
  val Block13 = new Val(13) with Block13t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block13Wrapper(key = newKey)
  }
  sealed case class Block13Wrapper(key: String) extends ValTWrapper(Block13) with ValTEmpty with Block13t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  sealed trait CommonBlock145 extends TopColor with LogoImg with BottomColor with LineColor with Title with Descr {
    override def topColorBf: BfColor = super.topColorBf.copy(
      defaultValue = Some("000000")
    )
    override def bottomColorBf: BfColor = super.bottomColorBf.copy(
      defaultValue = Some("bf6a6a")
    )
    override def lineColorBf: BfColor = super.lineColorBf.copy(
      defaultValue = Some("B35151")
    )
    val blockWidth: Int
  }

  sealed trait Block14t extends Height with CommonBlock145 {
    override def template = _block14Tpl
    override val blockWidth: Int = 300
    override def heightBf = super.heightBf.copy(
      availableVals = Set(300, 460)
    )
  }
  val Block14 = new Val(14) with Block14t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block14Wrapper(key = newKey)
  }
  sealed case class Block14Wrapper(key: String) extends ValTWrapper(Block14) with ValTEmpty with Block14t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }

  sealed trait Block15t extends CommonBlock145 {
    override def template = _block15Tpl
    override val blockWidth: Int = 140
  }
  val Block15 = new Val(15) with Block15t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block15Wrapper(key = newKey)
  }
  sealed case class Block15Wrapper(key: String) extends ValTWrapper(Block15) with ValTEmpty with Block15t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  sealed trait Block16t extends Height with BgColor with BorderColor with Title with Descr with Price {
    override def titleBf = super.titleBf.copy(
      withFontSizes = Set(65, 55, 45, 35, 28)
    )
    override def descrBf = super.descrBf.copy(
      withFontSizes = Set(36, 28, 22)
    )
    override def priceBf = super.priceBf.copy(
      withFontSizes = Set(65, 55, 45)
    )
    override def bgColorBf = super.bgColorBf.copy(
      defaultValue = Some("e1cea1")
    )

    override def template = _block16Tpl
  }
  val Block16 = new Val(16) with Block16t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block16Wrapper(key = newKey)
  }
  sealed case class Block16Wrapper(key: String) extends ValTWrapper(Block16) with ValTEmpty with Block16t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  sealed trait CommonBlock17_18 extends BgImg with BgColor with SaveBgImgI with CircleFillColor with Title
  with Discount with DiscoIconColor with DiscoBorderColor {
    override def bgColorBf = super.bgColorBf.copy(
      defaultValue = Some("FFFFFF")
    )
    override def discoIconColorBf = super.discoIconColorBf.copy(
      defaultValue = Some("ce2222")
    )
    override def circleFillColorBf = super.circleFillColorBf.copy(
      defaultValue = Some("f9daac")
    )
    val blockWidth: Int
  }


  sealed trait Block17t extends Height with CommonBlock17_18 {
    override val blockWidth: Int = 300
    override def heightBf: BfHeight = super.heightBf.copy(
      availableVals = Set(300, 460)
    )
    // Добавляем в начало формы поле высоты.
    override def blockFields: List[BlockFieldT] = heightBf :: super.blockFields
    override def template = _block17Tpl
  }
  val Block17 = new Val(17) with Block17t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block17Wrapper(key = newKey)
  }
  sealed case class Block17Wrapper(key: String) extends ValTWrapper(Block17) with ValTEmpty with Block17t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  sealed trait Block18t extends CommonBlock17_18 {
    override val blockWidth: Int = 140
    override def template = _block18Tpl
  }
  val Block18 = new Val(18) with Block18t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block18Wrapper(key = newKey)
  }
  sealed case class Block18Wrapper(key: String) extends ValTWrapper(Block18) with ValTEmpty with Block18t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }



  sealed trait Block19t extends Height with BgImg with BorderColor with TitlePriceListBlockT with BgColor with FillColor {
    override val offersCount = 2
    override def borderColorBf = super.borderColorBf.copy(
      defaultValue = Some("444444")
    )
    override def bgColorBf = super.bgColorBf.copy(
      defaultValue = Some("000000")
    )
    override def fillColorBf = super.fillColorBf.copy(
      defaultValue = Some("666666")
    )
    override def template = _block19Tpl
  }
  val Block19 = new Val(19) with Block19t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block19Wrapper(key = newKey)
  }
  sealed case class Block19Wrapper(key: String) extends ValTWrapper(Block19) with ValTEmpty with Block19t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  sealed trait Block20t extends Height with BgImg with Title with Descr {
    override def template = _block20Tpl
  }
  val Block20 = new Val(20) with Block20t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block20Wrapper(key = newKey)
  }
  sealed case class Block20Wrapper(key: String) extends ValTWrapper(Block20) with ValTEmpty with Block20t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  sealed trait Block21t extends Height with BgImg with BorderColor with Title with Descr {
    override def borderColorBf = super.borderColorBf.copy(
      defaultValue = Some("95FF00")
    )
    override def template = _block21Tpl
  }
  val Block21 = new Val(21) with Block21t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block21Wrapper(key = newKey)
  }
  sealed case class Block21Wrapper(key: String) extends ValTWrapper(Block21) with ValTEmpty with Block21t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  sealed trait Block22t extends Height with LogoImg with BorderColor with BgImg with Title with Descr {
    override def template = _block22Tpl
  }
  val Block22 = new Val(22) with Block22t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block22Wrapper(key = newKey)
  }
  sealed case class Block22Wrapper(key: String) extends ValTWrapper(Block22) with ValTEmpty with Block22t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  sealed trait Block23t extends BgImg with Title with Descr with Price with FillColor with HeightFixed {
    override def fillColorBf = super.fillColorBf.copy(
      defaultValue = Some("f3f3f3")
    )
    override def template = _block23Tpl
  }
  val Block23 = new Val(23) with Block23t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block23Wrapper(key = newKey)
  }
  sealed case class Block23Wrapper(key: String) extends ValTWrapper(Block23) with ValTEmpty with Block23t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  sealed trait Block24t extends LogoImg with BgImg with FillColor with Height with Title with Price with OldPrice {
    override def fillColorBf = super.fillColorBf.copy(
      defaultValue = Some("d5c864")
    )
    override def template = _block24Tpl
  }
  val Block24 = new Val(24) with Block24t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block24Wrapper(key = newKey)
  }
  sealed case class Block24Wrapper(key: String) extends ValTWrapper(Block24) with ValTEmpty with Block24t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


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
    // Собрать BindAcc и сконвертить в BlockMapperResult
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

