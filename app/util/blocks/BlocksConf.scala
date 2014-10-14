package util.blocks

import play.api.data._
import BlocksUtil._
import util.PlayMacroLogsImpl
import views.html.blocks._
import models._
import io.suggest.ym.model.common.BlockMeta
import util.blocks.BlocksUtil.BlockImgMap
import play.api.data.validation.Constraint
import util.img.{ImgIdKey, ImgInfo4Save}
import play.twirl.api.{HtmlFormat, Template3}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.14 16:50
 * Description: Конфиги для блоков верстки.
 *
 * 2014.may.20: По ряду различных причин, конфиги блоков были отрефакторены так, чтобы работать в режиме конструктора,
 * собираемого компилятором. Все поля -- это трейты (аддоны), которые можно добавить в блок через with/extends,
 * и компилятор разрулит автоматом все генераторы форм, маппинги и т.д.
 *
 * В связи со вспывшими подводными камнями, не удалось полностью избавится от ненужного кода, хотя оставшийся ненужный
 * код можно выправлять путём копипаста из другого блока. Конфиг блока состоит из нескольких частей:
 * - Базовый трейт. Обычно называется BlockNt (например Block19t, Block1t). Он содержит всё описание блока (все with).
 *   Разработка трейта такова:
 *     Смотрим на другой блок, делаем sealed "trait Block222t extends ... with ... with ... {}" по аналогии.
 *     Порядок полей в extends ... with ... with ... определяет порядок полей в форме.
 *     Разница в ролях ключевых слов "extends" и "with" в данном случае нивелируется, нужно просто помнить, что нужно
 *     первое поле трейта присоединяется через "extends X", а все последующие трейты полей -- через "with X".
 *     Например "trait Block222 extends Height with Title with Descr".
 *     Затем, если необходимо указать значения для конкретный полей блока (например defaultValue), внутри блока
 *     нажимаем alt+insert -> override -> выбираем нужное поле (например heightBf). После сгенеренного кода с
 *     super.heightBf добавляем .copy() и в скобках переопределяем значения полей. В итоге получается что-то типа:
 *     override def heightBf = super.heightBf.copy(
 *       defaultValue = Some(300),
 *       availableValse = Set(160, 300)
 *     )
 *
 * - val BlockN - неизменяемый инстанс элемента scala.Enumeration.Value и реализация базового трейта.
 *   Содержит в себе реализацию метода mappingWithNewKey(String), который генерит враппер над этим блоком для нужд\
 *   play mapping'ов.
 *   Разработка val Block: пишем что-то типа:
 *     val Block222 = new Val(222) with Block222t with EmptyKey {
 *       Внутрь копируем код метода mappingWithNewKey() из соседнего блока, заменив номер блока-враппера.
 *     }
 *
 * - BlockNWrapper (имеет имя Block1Wrapper, Block2Wrapper, итд). Вынужденный костыль, который отвязан от
 *   scala.Enumeration, но в остальном повторяет val BlockN. Является case-подклассом [[ValTWrapper]] и трейта BlockNt.
 *   Содержит в себе реализацию метода mappingWithNewKey(String), который вызывает copy() чтобы выставить врапперу новый
 *   mapping-ключ.
 *   Разработка враппера: копируем код враппера соседнего блока, заменив нужные числа на текущий номер блока.
 */

object BlocksConf extends Enumeration with PlayMacroLogsImpl {

  import LOGGER._

  /** Всё описание блока идёт через наследование Val и её интерфейса [[ValT]] при необходимости. */
  protected abstract class Val(id: Int) extends super.Val(id, "Block" + id) with ValTEmpty {

    /**
     * label'ы опций конфига блока, прописанные в conf/messages*.
     * @param bk исходный BK_-идентификатор
     * @return идентификатор, пригодный для резолва через Messages().
      */
    override def i18nLabelOf(bk: String) = I18N_PREFIX + bk

    /** Отрендерить редактор. */
    override def renderEditor(af: Form[_], formDataSer: Option[String])(implicit ctx: util.Context): HtmlFormat.Appendable = {
      editor._blockEditorTpl(af, withBC = Some(this), formDataSer = formDataSer)
    }
  }


  type BlockConf = Val
  implicit def value2val(x: Value): BlockConf = x.asInstanceOf[BlockConf]

  /** Найти опционально по имени. */
  def maybeWithName(n: String): Option[BlockConf] = {
    try {
      Some(withName(n))
    } catch {
      case ex: NoSuchElementException => None
    }
  }

  /** Дефолтовый блок, если возникают сомнения. */
  def DEFAULT: BlockConf = Block25

  /**
   * Аналог apply, но вызывает DEFAULT(), если нет блока с необходимым id.
   * @param n id искомого блока.
   * @return Экземпляр BlocksConf.
   */
  def applyOrDefault(n: Int): BlockConf = {
    try {
      apply(n)
    } catch {
      case ex: NoSuchElementException =>
        warn(s"BlockId is unknown: $n. Looks like, current MAd is deprecated.", ex)
        DEFAULT
    }
  }

  // Начало значений

  sealed trait Block20t extends Height with BgImg with TitleDescrListBlockT {
    override def ordering = 1000
    override def template = _block20Tpl
    override def heightBf = super.heightBf.copy(
      availableVals = Set(BfHeight.HEIGHT_140, BfHeight.HEIGHT_300, BfHeight.HEIGHT_460, BfHeight.HEIGHT_620)
    )
  }
  val Block20 = new Val(20) with Block20t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block20Wrapper(key = newKey)
  }
  sealed case class Block20Wrapper(key: String) extends ValTWrapper(Block20) with ValTEmpty with Block20t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  sealed trait Block23t extends BgImg with Title with Descr with Price with HeightFixed {
    override def template = _block23Tpl
  }
  val Block23 = new Val(23) with Block23t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block23Wrapper(key = newKey)
  }
  sealed case class Block23Wrapper(key: String) extends ValTWrapper(Block23) with ValTEmpty with Block23t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  sealed trait Block24t extends LogoImg with BgImg with FillColor with Height with Title with Price with OldPrice {
    override def isShown = false
    override def fillColorBf = super.fillColorBf.copy(
      defaultValue = Some("d5c864")
    )
    override def template = _block24Tpl
    override def blockWidth = BLOCK_WIDTH_NARROW_PX
  }
  val Block24 = new Val(24) with Block24t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block24Wrapper(key = newKey)
  }
  sealed case class Block24Wrapper(key: String) extends ValTWrapper(Block24) with ValTEmpty with Block24t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  sealed trait Block25t extends Height with BgImg with TitleDescrListBlockT {
    override def ordering = 1100
    override def template = _block25Tpl
    override def heightBf = super.heightBf.copy(
      availableVals = Set(BfHeight.HEIGHT_140, BfHeight.HEIGHT_300, BfHeight.HEIGHT_460, BfHeight.HEIGHT_620)
    )
    override def blockWidth = BLOCK_WIDTH_NARROW_PX
  }
  val Block25 = new Val(25) with Block25t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block25Wrapper(key = newKey)
  }
  sealed case class Block25Wrapper(key: String) extends ValTWrapper(Block25) with ValTEmpty with Block25t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  /** Блок-ссылка. Изначально создавался для пиара sioM. */
  sealed trait Block26t extends Height with BgImg with TitleDescrListBlockT with Href {
    override def isShown = true
    override def template = _block26Tpl
    override def offersCount: Int = 3
    override def heightBf = super.heightBf.copy(
      availableVals = Set(BfHeight.HEIGHT_140, BfHeight.HEIGHT_300, BfHeight.HEIGHT_460, BfHeight.HEIGHT_620)
    )
  }
  val Block26 = new Val(26) with Block26t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block26Wrapper(key = newKey)
  }
  sealed case class Block26Wrapper(key: String) extends ValTWrapper(Block26) with ValTEmpty with Block26t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  /** Отображаемые блоки. Обращение напрямую к values порождает множество с неопределённым порядком,
    * а тут - сразу отсортировано по id и только отображаемые. */
  val valuesShown: Seq[BlockConf] = {
    val vs0 = values.asInstanceOf[collection.Set[BlockConf]]
      .toSeq
      .filter(_.isShown)
    orderBlocks(vs0)
  }

  /** Отсортировать блоки согласно ordering с учётом id. */
  def orderBlocks(values: Seq[BlockConf]) = {
    values.sortBy { bc => bc.ordering -> bc.id }
  }
}


case class BlockMapperResult(bd: BlockData, bim: BlockImgMap) {
  def unapplyColor(bfc: BfColor): String = bd.colors.getOrElse(bfc.name, bfc.anyDefaultValue)
  def unapplyBIM(bfi: BfImage): BlockImgMap = bim.filter(_._1 == bfi.name)
  def flatMapFirstOffer[T](f: AOBlock => Option[T]) = bd.offers.headOption.flatMap(f)
}


/** Базовый интерфейс для реализаций класса Enumeration.Val. */
trait ValT extends ISaveImgs with Mapping[BlockMapperResult] {
  def id: Int

  def ordering: Int = 10000

  /** Ширина блока. Используется при дублировании блоков. */
  def blockWidth: Int = BLOCK_WIDTH_NORMAL_PX
  def isNarrow = blockWidth <= BLOCK_WIDTH_NARROW_PX

  def isShown = true

  /** Флаг того, что на блок не стоит навешивать скрипты, отрабатывающие клик по нему. */
  def hrefBlock = false

  /** Шаблон для рендера. */
  def template: Template3[MAdT, BlockRenderArgs, Context, HtmlFormat.Appendable]

  /** Набор маппингов для обработки данных от формы. */
  def strictMapping: Mapping[BlockMapperResult] = this

  /** Более удобный интерфейс для метода template.render(). */
  def renderBlock(mad: MAdT, args: BlockRenderArgs = BlockRenderArgs.DEFAULT)(implicit ctx: Context) = {
    template.render(mad, args, ctx)
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

