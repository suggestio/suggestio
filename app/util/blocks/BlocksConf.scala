package util.blocks

import play.api.templates._
import util.Context
import play.api.data._, Forms._
import util.FormUtil._
import views.html.blocks._
import BlocksUtil._
import io.suggest.model.EsModel
import models._

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
    def renderBlock(bm: BlockMap, isStandalone: Boolean)(implicit ctx: Context) = {
      template.render(bm, isStandalone, ctx)
    }

    /** При генерации рендера-превьюшки нет доступа к AORaw и т.д. Извлекаем карту из формы. */
    def renderEditorPreview(bform: Form[_], nameBase: String)(implicit ctx: Context) = {
      val bm = bform.data.foldLeft[List[(String,Any)]] (Nil) { case (acc, (k, v)) =>
        if (k.startsWith(nameBase)) {
          val newk = k.substring(nameBase.length)
          val v1 = Option(v)
            .filter(!_.isEmpty)
            .orElse {
              blockFields.find(_.name == newk).flatMap(_.defaultValue)
            }
            .getOrElse(v)
          newk -> v1 :: acc
        } else {
          acc
        }
      }
      renderBlock(bmEnsureId(bm.toMap), isStandalone = false)
    }

    def bmEnsureId(bm: BlockMap): BlockMap = bm + (BK_BLOCK_ID -> id)

    def aoRawMapping = bMapping
      .transform[AORaw](
       {bm => AORaw( bmEnsureId(bm) ) },
        _.bodyMap
      )

    /**
     * label'ы опций конфига блока, прописанные в conf/messages*.
     * @param bk исходный BK_-идентификатор
     * @return идентификатор, пригодный для резолва через Messages().
      */
    def i18nLabelOf(bk: String) = "blocks.bk." + bk

    /** Описание используемых полей. На основе этой спеки генерится шаблон формы редактора. */
    def blockFields: List[BlockFieldT]
  }

  type BlockConf = Val
  implicit def value2val(x: Value): BlockConf = x.asInstanceOf[BlockConf]


  // Начало значений

  val Block1 = new Val(1, "verySimple") {
    val heightField = NumberBlockField(BK_HEIGHT, BlocksEditorFields.Height, minValue = 140, maxValue=460, defaultValue = Some(140))
    val titleField = StringBlockField(BK_TITLE, BlocksEditorFields.InputText, maxLen = 512)
    val descrField = StringBlockField(BK_DESCRIPTION, BlocksEditorFields.TextArea, maxLen = 8192)

    override val blockFields = List(
      heightField, titleField, descrField
    )

    /** Набор маппингов для обработки данных от формы. */
    override val bMapping = mapping(
      BK_HEIGHT       -> heightField.getMapping,
      BK_TITLE        -> titleField.getMapping,
      BK_DESCRIPTION  -> descrField.getMapping
    )
    {(height, title, descr) =>
      Map(BK_HEIGHT -> height, BK_TITLE -> title, BK_DESCRIPTION -> descr): BlockMap
    }
    {bmap =>
      val bmapSafe = bmap.withDefault {
        case BK_HEIGHT => heightField.defaultValue.getOrElse(140)
        case _         => ""
      }
      val height = EsModel.intParser(bmapSafe(BK_HEIGHT))
      val title = bmapSafe(BK_TITLE).toString
      val descr = bmapSafe(BK_DESCRIPTION).toString
      Some((height, title, descr))
    }

    /** Шаблон для рендера. */
    override def template = _block1Tpl

  }


  // Хелперы

  def maybeWithName(n: String): Option[BlockConf] = {
    try {
      Some(withName(n))
    } catch {
      case ex: NoSuchElementException => None
    }
  }


  def renderBlockMap(bm: BlockMap, isStandalone: Boolean)(implicit ctx: Context) = {
    val blockId = BlocksUtil.extractBlockId(bm)
    val blockConf = apply(blockId)
    blockConf.renderBlock(bm, isStandalone)
  }
  
}
