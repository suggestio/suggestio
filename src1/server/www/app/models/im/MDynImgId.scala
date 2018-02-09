package models.im

import java.util.UUID

import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.img.{MImgFmt, MImgFmts}
import io.suggest.util.UuidUtil
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.18 16:26
  * Description: Модель идентификатора dyn-картинки.
  * Является контейнером, вынесенный за пределы MImg* моделей, чтобы унифицировать передачу пачек данных конструкторов.
  */
case class MDynImgId(
                      rowKeyStr     : String,
                      dynImgOps     : Seq[ImOp]     = Nil
                    )
{

  def format: MImgFmt = MImgFmts.JPEG

  // TODO Это надо для старых и очень картинок. Всякие GalleryUtil, LogoUtil и прочие зависят от этого.
  lazy val rowKey: UUID = UuidUtil.base64ToUuid(rowKeyStr)

  def hasImgOps = dynImgOps.nonEmpty

  def original: MDynImgId = {
    if (hasImgOps)
      withDynImgOps(Nil)
    else
      this
  }

  def withDynImgOps(dynImgOps: Seq[ImOp] = Nil) = copy(dynImgOps = dynImgOps)

  /** id для модели MMedia. */
  lazy val mediaId: String = {
    MDynImgId.mkMediaId(rowKeyStr, qOpt, format)
  }

  def qOpt: Option[String] = {
    OptionUtil.maybe(hasImgOps)(dynImgOpsString)
  }


  // Быдлокод скопирован из MImgT.
  def dynImgOpsStringSb(sb: StringBuilder = ImOp.unbindSbDflt): StringBuilder = {
    ImOp.unbindImOpsSb(
      keyDotted     = "",
      value         = dynImgOps,
      withOrderInx  = false,
      sb            = sb
    )
  }

  lazy val dynImgOpsString: String = {
    dynImgOpsStringSb()
      .toString()
  }

}


object MDynImgId {

  /**
    * Сборка id'шников для экземпляров модели, хранящих динамические изображения.
    *
    * 2018-02-09 В связи с внедрением формата картинок, новые правила генерации id, совместимые со старыми:
    * - Оригиналы чего угодно всегда без формата (как раньше).
    * - Картинки-деривативы:
    *   - Если JPEG, то без формата в id (как раньше).
    *   - формат указывается явно в id во всех остальных случаях (новые PNG, GIF и т.д.).
    *
    * Примеры:
    * "afw43faw4ffw"              // Оригинальный файл в оригинальном формате, указанном внутри MMedia.fileMeta.
    * "afw43faw4ffw?a=x&b=e"      // JPEG-дериватив из оригинала "afw43faw4ffw".
    * "afw43faw4ffw.png?a=x&b=e"  // PNG-дериватив из оригинала "afw43faw4ffw".
    *
    * @param rowKeyStr id ноды картинки.
    * @param qOpt Опциональный qualifier. Обычно None, если это файл-оригинал.
    *             Some() если хранится дериватив.
    * @return Строка для поля _id.
    */
  def mkMediaId(rowKeyStr: String, qOpt: Option[String], format: MImgFmt): String = {
    var acc: List[String] = Nil

    // Строка с модификаторами.
    for (q <- qOpt)
      acc = "?" :: q :: acc

    // Эктеншен формата картинки, если требуется.
    val isOrig = qOpt.isEmpty
    if (!isOrig && format !=* MImgFmts.JPEG)
      acc = HtmlConstants.`.` :: format.fileExt :: acc

    // Финальная сборка полного id.
    if (acc.isEmpty)
      rowKeyStr
    else
      (rowKeyStr :: acc).mkString
  }


}
