package models.im

import java.util.UUID

import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.compress.MCompressAlgo
import io.suggest.img.crop.MCrop
import io.suggest.img.MImgFmt
import io.suggest.jd.MJdEdgeId
import io.suggest.model.n2.edge.MEdge
import io.suggest.util.UuidUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.18 16:26
  * Description: Модель идентификатора dyn-картинки.
  * Является контейнером, вынесенный за пределы MImg* моделей, чтобы унифицировать передачу пачек данных конструкторов.
  */

/** Контейнер данных для идентификации картинки.
  *
  * @param rowKeyStr Ключ (id узла-картинки).
  * @param dynFormat Динамический формат картинки.
  * @param dynImgOps IM-операции, которые нужно наложить на оригинал с ключом rowKey, чтобы получить
  *                  необходимою картинку.
  * @param compressAlgo Опциональный финальный алгоритм сжатия.
  *                     Нужен для сборки SVG, пожатых через brotli или иным алгоритмом.
  */
final case class MDynImgId(
                            rowKeyStr     : String,
                            dynFormat     : MImgFmt,
                            dynImgOps     : Seq[ImOp]             = Nil,
                            compressAlgo  : Option[MCompressAlgo] = None
                            // TODO svgo=()?
                          ) {

  // TODO Переименовать поле rowKeyStr в nodeId.
  def nodeId: String = rowKeyStr

  // TODO Допилить и активировать ассерты правил применения формата изображения.
  // assert( hasImgOps && dynFormat.nonEmpty )

  /** Ключ ряда картинок, id для оригинала и всех производных. */
  lazy val rowKey: UUID = UuidUtil.base64ToUuid(rowKeyStr)

  /** Нащупать crop. Используется скорее как compat к прошлой форме работы с картинками. */
  def cropOpt: Option[MCrop] = {
    val iter = dynImgOps
      .iterator
      .flatMap {
        case AbsCropOp(crop) => crop :: Nil
        case _ => Nil
      }
    OptionUtil.maybe(iter.hasNext)( iter.next() )
  }

  def isCropped: Boolean = {
    dynImgOps
      .exists { _.isInstanceOf[ImCropOpT] }
  }

  def hasImgOps: Boolean = dynImgOps.nonEmpty

  lazy val fileName: String = fileNameSb().toString()

  /**
   * Билдер filename-строки
   * @param sb Исходный StringBuilder.
   * @return StringBuilder.
   */
  def fileNameSb(sb: StringBuilder = new StringBuilder(80)): StringBuilder = {
    sb.append(rowKeyStr)
    if (hasImgOps) {
      sb.append('~')
      dynImgOpsStringSb(sb)
    }
    sb.append( '.' )
      .append( dynFormat.fileExt )
    for (algo <- compressAlgo) {
      sb.append('.')
        .append(algo.fileExtension)
    }
    sb
  }

  /** Хранилка инстанса оригинала.
    * Для защиты от хранения ненужных ссылок на this, тут связка из метода и lazy val. */
  private lazy val _originalHolder = withDynImgOps(Nil)
  def original: MDynImgId = {
    if (hasImgOps)
      _originalHolder
    else
      this
  }
  def maybeOriginal: Option[MDynImgId] = {
    OptionUtil.maybe(hasImgOps)(_originalHolder)
  }

  def mediaIdWithOriginalMediaId: Seq[String] = {
    Stream.cons(
      mediaId,
      maybeOriginal.fold(Stream.empty[String]) { d =>
        d.mediaId #:: Stream.empty[String]
      }
    )
  }

  def withDynImgOps(dynImgOps: Seq[ImOp] = Nil) = copy(dynImgOps = dynImgOps)
  def withDynFormat(dynFormat: MImgFmt)         = copy(dynFormat = dynFormat)

  /** Добавить операции в конец списка операций. */
  def addDynImgOps(addDynImgOps: Seq[ImOp]): MDynImgId = {
    if (addDynImgOps.isEmpty) {
      this
    } else if (dynImgOps.isEmpty) {
      withDynImgOps( addDynImgOps )
    } else {
      withDynImgOps( dynImgOps ++ addDynImgOps )
    }
  }

  /** id для модели MMedia. */
  lazy val mediaId = MDynImgId.mkMediaId(this)

  /** Исторически, это column qualifier, который использовался в column-oriented dbms.
    * Сейчас используется тоже в качестве id, либо части id.
    */
  def qOpt: Option[String] = {
    // TODO XXX dynFormat, compressAlgo
    OptionUtil.maybe(hasImgOps)(dynImgOpsString)
  }


  // Быдлокод скопирован из MImgT.
  def dynImgOpsStringSb(sb: StringBuilder = ImOp.unbindSbDflt): StringBuilder = {
    // TODO XXX dynFormat, compressAlgo
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


  lazy val fsFileName: String = {
    if (hasImgOps) {
      // TODO Тут надо формат дописать?
      dynImgOpsString + "." + dynFormat.fileExt
    } else {
      "__ORIG__"
    }
  }

}


object MDynImgId {

  /** Рандомный id для нового оригинала картинки. */
  def randomOrig(dynFormat: MImgFmt) = MDynImgId(
    rowKeyStr = UuidUtil.uuidToBase64( UUID.randomUUID() ),
    dynFormat = dynFormat
  )

  /**
    * Сборка id'шников для экземпляров модели, хранящих динамические изображения.
    *
    * 2018-02-09 В связи с внедрением формата картинок, после rowKeyStr указывается расширение файла файлового формата:
    *
    * Примеры:
    * "afw43faw4ffw"                // Оригинальный файл в оригинальном формате.
    * "afw43faw4ffw.jpeg?a=x&b=e"   // JPEG-дериватив из оригинала "afw43faw4ffw".
    */
  def mkMediaId(dynImgId: MDynImgId): String = {
    var acc: List[String] = Nil

    // Строка с модификаторами.
    val qOpt = dynImgId.qOpt
    for (q <- qOpt)
      acc = "?" :: q :: acc

    // Эктеншен формата картинки, если не-оригинал.
    if (dynImgId.hasImgOps)
      acc = HtmlConstants.`.` :: dynImgId.dynFormat.fileExt :: acc

    // Финальная сборка полного id.
    if (acc.isEmpty)
      dynImgId.rowKeyStr
    else
      (dynImgId.rowKeyStr :: acc).mkString
  }


  /** Экстрактор данных [[MDynImgId]] из jd-эджа в связке с обычным эджем.
    * Метод не проверяет связно
    *
    * @param jdId Данные jd-эджа.
    * @param medge Связанный MEdge.
    * @return
    */
  def fromJdEdge(jdId: MJdEdgeId, medge: MEdge): MDynImgId = {
    apply(
      rowKeyStr = medge.nodeIds.head,
      dynFormat = jdId.outImgFormat.get,
      dynImgOps = {
        var acc = List.empty[ImOp]
        for (mcrop <- jdId.crop)
          acc ::= AbsCropOp( mcrop )
        acc
      }
    )
  }

}
