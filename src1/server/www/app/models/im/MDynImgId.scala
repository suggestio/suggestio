package models.im

import java.util.UUID

import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.compress.MCompressAlgo
import io.suggest.img.crop.MCrop
import io.suggest.img.MImgFormat
import io.suggest.jd.MJdEdgeId
import io.suggest.n2.edge.MEdge
import io.suggest.util.UuidUtil
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.18 16:26
  * Description: Модель уникального имени/пути/идентификатора файла (не обязательно локального), в частности картинки.
  * Подразумевается, что файл может быть производным от некоего исходника, поэтому в идентификаторе
  * закладываются поля для формата, IM-трансформаций и т.д.
  * Также используется, как id узлов.
  *
  * До 2020.07.02 - это модель идентификатора картинки.
  */
object MDynImgId {

  /** Рандомный id для нового оригинала файла. */
  def randomId(): String =
    UuidUtil.uuidToBase64( UUID.randomUUID() )

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
    for (imgFormat <- dynImgId.imgFormat if dynImgId.hasImgOps)
      acc = HtmlConstants.`.` :: imgFormat.fileExt :: acc

    // Финальная сборка полного id.
    if (acc.isEmpty)
      dynImgId.origNodeId
    else
      (dynImgId.origNodeId :: acc).mkString
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
      origNodeId = medge.nodeIds.head,
      imgFormat = jdId.outImgFormat,
      imgOps = {
        var acc = List.empty[ImOp]
        for (mcrop <- jdId.crop)
          acc ::= AbsCropOp( mcrop )
        acc
      }
    )
  }


  def rowKeyStr = GenLens[MDynImgId](_.origNodeId)
  def imgFormat = GenLens[MDynImgId](_.imgFormat)
  def dynImgOps = GenLens[MDynImgId](_.imgOps)
  def compressAlgo = GenLens[MDynImgId](_.compressAlgo)


  implicit class DynImgIdOpsExt( private val dynImgId: MDynImgId) extends AnyVal {

    /** Нащупать crop. Используется скорее как compat к прошлой форме работы с картинками. */
    def cropOpt: Option[MCrop] = {
      val iter = dynImgId
        .imgOps
        .iterator
        .flatMap {
          case AbsCropOp(crop) => crop :: Nil
          case _ => Nil
        }
      OptionUtil.maybe(iter.hasNext)( iter.next() )
    }

    def isCropped: Boolean = {
      dynImgId
        .imgOps
        .exists { _.isInstanceOf[ImCropOpT] }
    }

    def hasImgOps: Boolean = dynImgId.imgOps.nonEmpty
    def isOriginal = !hasImgOps

    def original: MDynImgId = {
      if (hasImgOps) dynImgId._originalHolder
      else dynImgId
    }

    def maybeOriginal: Option[MDynImgId] = {
      OptionUtil.maybe(hasImgOps)( dynImgId._originalHolder )
    }

    def imgIdAndOrig: Seq[MDynImgId] = {
      val ll0 = LazyList.empty[MDynImgId]
      dynImgId #:: {
        maybeOriginal.fold(ll0) { d =>
          d #:: ll0
        }
      }
    }

    def mediaIdAndOrigMediaId: Seq[String] =
      imgIdAndOrig.map(_.mediaId)


    /** Добавить операции в конец списка операций. */
    def addDynImgOps(addDynImgOps: Seq[ImOp]): MDynImgId = {
      if (addDynImgOps.isEmpty) {
        dynImgId
      } else {
        val ops2 =
          if (dynImgId.imgOps.isEmpty) addDynImgOps
          else dynImgId.imgOps ++ addDynImgOps
        (MDynImgId.dynImgOps set ops2)(dynImgId)
      }
    }

    /** Исторически, это column qualifier, который использовался в column-oriented dbms.
      * Сейчас используется тоже в качестве id, либо части id.
      */
    def qOpt: Option[String] = {
      // TODO XXX dynFormat, compressAlgo
      OptionUtil.maybe(hasImgOps)( dynImgId.dynImgOpsString )
    }

  }

}


/** Контейнер данных для идентификации картинки.
  *
  * @param origNodeId id узла-картинки оригинала. Обычный id узла без dynOps-суффиксов.
  * @param imgFormat Динамический формат картинки.
  * @param imgOps IM-операции, которые нужно наложить на оригинал с ключом rowKey, чтобы получить
  *                  необходимою картинку.
  * @param compressAlgo Опциональный финальный алгоритм сжатия.
  *                     Нужен для сборки SVG, пожатых через brotli или иным алгоритмом.
  */
final case class MDynImgId(
                            origNodeId    : String,
                            // 2020-07-03 Модель теперь используется не только для картинок. dynFormat опционален.
                            imgFormat     : Option[MImgFormat]    = None,
                            imgOps        : Seq[ImOp]             = Nil,
                            compressAlgo  : Option[MCompressAlgo] = None
                            // TODO svgo=()?
                          ) {

  // TODO Допилить и активировать ассерты правил применения формата изображения.
  // assert( dynFormat.nonEmpty )

  lazy val fileName: String = {
    val sb: StringBuilder = new StringBuilder(80)

    sb.append( origNodeId )
    if (imgOps.nonEmpty) {
      sb.append('~')
        .append( dynImgOpsString )
    }

    for (imgFmt <- imgFormat) {
      sb.append( '.' )
        .append( imgFmt.fileExt )
    }


    for (algo <- compressAlgo) {
      sb.append( '.' )
        .append( algo.fileExtension )
    }

    sb.toString()
  }

  /** Хранилка инстанса оригинала.
    * Для защиты от хранения ненужных ссылок на this, тут связка из метода и lazy val. */
  private lazy val _originalHolder =
    (MDynImgId.dynImgOps set Nil)(this)

  /** id для модели MMedia. */
  lazy val mediaId = MDynImgId.mkMediaId(this)

  lazy val dynImgOpsString: String = {
    // TODO XXX dynFormat, compressAlgo
    ImOp.unbindImOps(
      keyDotted     = "",
      value         = imgOps,
      withOrderInx  = false,
    )
  }

  lazy val fsFileName: String = {
    if (this.hasImgOps) {
      // TODO Тут надо формат дописать?
      var r = dynImgOpsString

      for (imgFmt <- imgFormat)
        r = r + "." + imgFmt.fileExt

      r
    } else {
      "__ORIG__"
    }
  }

  override def toString = fileName

}

