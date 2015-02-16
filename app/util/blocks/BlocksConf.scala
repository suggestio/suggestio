package util.blocks

import models.blk.{BlockHeights, BlockWidths}
import play.api.data._
import BlocksUtil._
import util.PlayMacroLogsImpl
import views.html.blocks._
import models._
import io.suggest.ym.model.common.BlockMeta
import util.blocks.BlocksUtil.BlockImgMap
import play.api.data.validation.Constraint
import play.twirl.api.{Html, Template3}

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
    override def i18nLabelOf(bk: String) = "blocks.field." + bk

    /** Отрендерить редактор. */
    override def renderEditor(af: AdFormM, formDataSer: Option[String])(implicit ctx: Context): Html = {
      editor._blockEditorTpl(af, withBC = Some(this), formDataSer = formDataSer)
    }
  }


  type BlockConf = ValT
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
  def DEFAULT: CommonBlock2T = Block20

  /**
   * Аналог apply, но вызывает DEFAULT(), если нет блока с необходимым id.
   * @param n id искомого блока.
   * @return Экземпляр BlocksConf.
   */
  def applyOrDefault(n: Int): CommonBlock2T = {
    try {
      apply(n).asInstanceOf[CommonBlock2T]
    } catch {
      case ex: NoSuchElementException =>
        debug(s"BlockId is unknown: $n. Looks like, current MAd need to be resaved via editor.")
        DEFAULT
    }
  }

  // Начало значений

  /** Блок рекламной карточки с произвольным заполнением и без svg. */
  sealed trait CommonBlock2T extends Height with Width with BgImg with TitleListBlockT with Href with IsWideBg

  /** Блок рекламной карточки с произвольным заполнением и без svg. */
  sealed trait Block20t extends CommonBlock2T {
    override def ordering = 1000
    override def template = _block20Tpl
  }
  val Block20 = new Val(20) with Block20t with EmptyKey {
    override def mappingWithNewKey(newKey: String) = Block20Wrapper(key = newKey)
  }
  sealed case class Block20Wrapper(key: String) extends ValTWrapper(Block20) with ValTEmpty with Block20t {
    override def mappingWithNewKey(newKey: String) = copy(key = newKey)
  }


  // Конец значений. Уже. А ведь когда-то их было 26...


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

  def isShown = true

  /** Флаг того, что на блок не стоит навешивать скрипты, отрабатывающие клик по нему. */
  def hrefBlock = false

  /** Шаблон для рендера. */
  def template: Template3[MAdT, blk.RenderArgs, Context, Html]

  /** Набор маппингов для обработки данных от формы. */
  def strictMapping: Mapping[BlockMapperResult] = this

  /** Более удобный интерфейс для метода template.render(). */
  def renderBlock(mad: MAdT, args: blk.RenderArgs)(implicit ctx: Context) = {
    template.render(mad, args, ctx)
  }

  /**
   * label'ы опций конфига блока, прописанные в conf/messages*.
   * @param bk исходный BK_-идентификатор
   * @return идентификатор, пригодный для резолва через Messages().
   */
  def i18nLabelOf(bk: String): String

  /** Stackable-trait: заполняется в прямом порядке в отличии от списка blockFields().
    * Этот метод помогает заполнять список ВСЕХ полей задом наперёд. */
  def blockFieldsRev(af: AdFormM): List[BlockFieldT]

  /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
  def blockFields(af: AdFormM): List[BlockFieldT] = blockFieldsRev(af).reverse

  /** Поиск поля картинки для указанного имени поля. */
  def getImgFieldForName(fn: String): Option[BfImage] = None

  /** Отрендерить редактор. */
  def renderEditor(af: AdFormM, formDataSer: Option[String])(implicit ctx: Context): Html

  // Mapping:
  def mappingsAcc: List[Mapping[_]]
  override val mappings = mappingsAcc

  override val constraints: Seq[Constraint[BlockMapperResult]] = Nil
  override def verifying(constraints: Constraint[BlockMapperResult]*): Mapping[BlockMapperResult] = {
    throw new UnsupportedOperationException("verifying() never implemented for BlockConf.")
  }

  def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc]
  override def bind(data: Map[String, String]): Either[Seq[FormError], BlockMapperResult] = {
    // Собрать BindAcc и сконвертить в BlockMapperResult
    bindAcc(data).right.map { bindAcc =>
      val bd = BlockDataImpl(
        blockMeta = bindAcc.toBlockMeta(id),
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
  var colors  : List[(String, String)] = Nil,
  var offers  : List[AOBlock] = Nil,
  var height  : Int = BlockHeights.default.heightPx,
  var width   : Int = BlockWidths.default.widthPx,
  var isWide  : Boolean = false,
  var bim     : List[BlockImgEntry] = Nil
) {

  /**
   * Данные этого аккб, относящиеся к метаданным блока, скомпилить в экземпляр BlockMeta.
   * @param blockId id блока.
   * @return Неизменяемый экземпляр BlockMeta.
   */
  def toBlockMeta(blockId: Int) = BlockMeta(blockId = blockId, height = height, width = width, wide = isWide)

}


abstract class ValTWrapper(v: ValT) extends ValT {
  override def id = v.id
  override def i18nLabelOf(bk: String) = v.i18nLabelOf(bk)
  override def renderEditor(af: AdFormM, formDataSer: Option[String])(implicit ctx: Context): Html = {
    v.renderEditor(af, formDataSer)
  }
}


/** Враппер понадобился из-за проблем со scala.Enumeration, который не даёт делать инстансы Val несколько раз. */
trait ValTEmpty extends ValT {
  override def blockFieldsRev(af: AdFormM): List[BlockFieldT] = Nil
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

