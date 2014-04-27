package util.blocks

import play.api.templates.{Template3, HtmlFormat, Template2}
import util.Context
import play.api.data._, Forms._
import util.FormUtil._
import views.html.blocks._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.14 16:50
 * Description: Конфиги для блоков верстки и вспомогательная утиль.
 */

object BlocksUtil {
  type BlockMap = Map[String, Any]

  // BK-константы именуют все используемые ключи конфигов. Полезно для избежания ошибок в разных местах.
  val BK_HEIGHT       = "height"
  val BK_TITLE        = "title"
  val BK_DESCRIPTION  = "description"
  val BK_PHOTO_ID     = "photoId"
  val BK_BG_COLOR     = "bgColor"

  val bTitleM = nonEmptyText(minLength = 2, maxLength = 250)
    .transform[String](strTrimSanitizeF, strIdentityF)

  val bDescriptionM = publishedTextM
}


import BlocksUtil._

/** Enum, описывающий все допустимые конфигурации. */
object BlocksConf extends Enumeration {
  
  /** Всё описание блока идёт через наследование Val. */
  protected abstract case class Val(n: Int, name: String) extends super.Val(n, name) {
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

    /** Добавить в карту фунцию для получения дефолтовых значений.
      * Для перезаписи BK_HEIGHT следует использовать [[bkHeightDflt]]. */
    def withDefaultMap(confMap: BlockMap) = confMap.withDefault(defaultValueFor)

    /** Дефолтовые значения для ключей конфигурации блока. */
    def defaultValueFor(bk: String): Any = {
      if (bk == BK_HEIGHT)
        bkHeightDflt
      else
        throw new NoSuchElementException(s"Key '$bk' not found.")
    }

    /**
     * label'ы опций конфига блока, прописанные в conf/messages*.
     * @param bk исходный BK_-идентификатор
     * @return идентификатор, пригодный для резолва через Messages().
      */
    def i18nLabelOf(bk: String) = "blocks.bk." + bk
  }

  type BlockConf = Val
  implicit def value2val(x: Value): BlockConf = x.asInstanceOf[BlockConf]


  // Начало значений

  val Block1 = new Val(1, "1") {
    /** Набор маппингов для обработки данных от формы. */
    def bMapping = mapping[BlockMap, String, String](
      BK_TITLE -> bTitleM,
      BK_DESCRIPTION -> bDescriptionM
    )
    {(title, descr) =>
      Map(BK_TITLE -> title, BK_DESCRIPTION -> descr)
    }
    {bmap =>
      val bmapSafe = bmap.withDefault { _ => "" }
      Some( (bmapSafe(BK_TITLE).toString, bmapSafe(BK_DESCRIPTION).toString) )
    }

    /** Шаблон для рендера. */
    def template = _block1Tpl
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
