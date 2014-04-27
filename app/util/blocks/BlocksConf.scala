package util.blocks

import play.api.templates._
import util.Context
import play.api.data._, Forms._
import util.FormUtil._
import views.html.blocks._
import BlocksUtil._

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
    def template: Template3[BlockMap, Boolean, Context, HtmlFormat.Appendable]

    /** Набор маппингов для обработки данных от формы. */
    def bMapping: Mapping[BlockMap]

    /** Более удобный интерфейс для метода template.render(). */
    def render(confMap: BlockMap, isStandalone: Boolean)(implicit ctx: Context) = {
      template.render(confMap, isStandalone, ctx)
    }

    /** Высота блока есть везде, поэтому её дефолтовое значение вынесено сюда. */
    def bkHeightDflt = 140
    def bkHeightMin = bkHeightDflt
    def bkHeightMax = 460

    /** Добавить в карту фунцию для получения дефолтовых значений.
      * Для перезаписи BK_HEIGHT следует использовать [[bkHeightDflt]]. */
    def withDefaultMap(confMap: BlockMap) = confMap.withDefault(defaultValueFor)

    def maybeDefaultValueFor(bk: String): Option[Any] = {
      if (bk == BK_HEIGHT)
        Some(bkHeightDflt)
      else
        None
    }

    def minValueFor(bk: String): Option[Any] = {
      if (bk == BK_HEIGHT)
        Some(bkHeightMin)
      else
        None
    }

    def maxValueFor(bk: String): Option[Any] = {
      if (bk == BK_HEIGHT)
        Some(bkHeightMax)
      else
        None
    }

    def heightM = number(
      min = bkHeightMin,
      max = bkHeightMax
    )

    /** Дефолтовые значения для ключей конфигурации блока. */
    def defaultValueFor(bk: String): Any = {
      maybeDefaultValueFor(bk).get
    }

    /**
     * label'ы опций конфига блока, прописанные в conf/messages*.
     * @param bk исходный BK_-идентификатор
     * @return идентификатор, пригодный для резолва через Messages().
      */
    def i18nLabelOf(bk: String) = "blocks.bk." + bk

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    def blockFields: List[BlockField]
  }

  type BlockConf = Val
  implicit def value2val(x: Value): BlockConf = x.asInstanceOf[BlockConf]


  // Начало значений

  val Block1 = new Val(1, "verySimple") {
    /** Набор маппингов для обработки данных от формы. */
    override val bMapping = mapping(
      BK_HEIGHT       -> heightM,
      BK_TITLE        -> bTitleM,
      BK_DESCRIPTION  -> bDescriptionM
    )
    {(height, title, descr) =>
      Map(BK_HEIGHT -> height, BK_TITLE -> title, BK_DESCRIPTION -> descr): BlockMap
    }
    {bmap =>
      val bmapSafe = bmap.withDefault {
        case BK_HEIGHT => bkHeightDflt
        case _         => ""
      }
      val height = bmapSafe(BK_HEIGHT).asInstanceOf[Int]
      val title = bmapSafe(BK_TITLE).toString
      val descr = bmapSafe(BK_DESCRIPTION).toString
      Some((height, title, descr))
    }

    /** Шаблон для рендера. */
    override def template = _block1Tpl

    override val blockFields = List(
      BlockField(BK_HEIGHT, BlocksEditorFields.Height),
      BlockField(BK_TITLE, BlocksEditorFields.InputText),
      BlockField(BK_DESCRIPTION, BlocksEditorFields.TextArea)
    )
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
