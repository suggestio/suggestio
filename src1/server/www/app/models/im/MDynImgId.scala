package models.im

import java.util.UUID

import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.compress.MCompressAlgo
import io.suggest.img.crop.MCrop
import io.suggest.img.MImgFmt
import io.suggest.jd.MJdEdgeId
import io.suggest.n2.edge.MEdge
import io.suggest.util.UuidUtil
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.18 16:26
  * Description: Модель идентификатора dyn-картинки.
  * Является контейнером, вынесенный за пределы MImg* моделей, чтобы унифицировать передачу пачек данных конструкторов.
  */
object MDynImgId {

  /** Рандомный id для нового оригинала картинки. */
  def randomOrig(dynFormat: MImgFmt) = MDynImgId(
    origNodeId = UuidUtil.uuidToBase64( UUID.randomUUID() ),
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
      dynFormat = jdId.outImgFormat.get,
      dynImgOps = {
        var acc = List.empty[ImOp]
        for (mcrop <- jdId.crop)
          acc ::= AbsCropOp( mcrop )
        acc
      }
    )
  }


  def rowKeyStr = GenLens[MDynImgId](_.origNodeId)
  def dynFormat = GenLens[MDynImgId](_.dynFormat)
  def dynImgOps = GenLens[MDynImgId](_.dynImgOps)
  def compressAlgo = GenLens[MDynImgId](_.compressAlgo)


  implicit class DynImgIdOpsExt( private val dynImgId: MDynImgId) extends AnyVal {

    /** Нащупать crop. Используется скорее как compat к прошлой форме работы с картинками. */
    def cropOpt: Option[MCrop] = {
      val iter = dynImgId
        .dynImgOps
        .iterator
        .flatMap {
          case AbsCropOp(crop) => crop :: Nil
          case _ => Nil
        }
      OptionUtil.maybe(iter.hasNext)( iter.next() )
    }

    def isCropped: Boolean = {
      dynImgId
        .dynImgOps
        .exists { _.isInstanceOf[ImCropOpT] }
    }

    def hasImgOps: Boolean = dynImgId.dynImgOps.nonEmpty
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
          if (dynImgId.dynImgOps.isEmpty) addDynImgOps
          else dynImgId.dynImgOps ++ addDynImgOps
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
  * @param dynFormat Динамический формат картинки.
  * @param dynImgOps IM-операции, которые нужно наложить на оригинал с ключом rowKey, чтобы получить
  *                  необходимою картинку.
  * @param compressAlgo Опциональный финальный алгоритм сжатия.
  *                     Нужен для сборки SVG, пожатых через brotli или иным алгоритмом.
  */
final case class MDynImgId(
                            origNodeId    : String,
                            dynFormat     : MImgFmt,
                            dynImgOps     : Seq[ImOp]             = Nil,
                            compressAlgo  : Option[MCompressAlgo] = None
                            // TODO svgo=()?
                          ) {

  // TODO Допилить и активировать ассерты правил применения формата изображения.
  // assert( dynFormat.nonEmpty )

  lazy val fileName: String = {
    val sb: StringBuilder = new StringBuilder(80)

    sb.append( origNodeId )
    if (dynImgOps.nonEmpty) {
      sb.append('~')
        .append( dynImgOpsString )
    }

    val dot = '.'
    sb.append( dot )
      .append( dynFormat.fileExt )

    for (algo <- compressAlgo) {
      sb.append(dot)
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
      value         = dynImgOps,
      withOrderInx  = false,
    )
  }

  lazy val fsFileName: String = {
    if (this.hasImgOps) {
      // TODO Тут надо формат дописать?
      dynImgOpsString + "." + dynFormat.fileExt
    } else {
      "__ORIG__"
    }
  }

}

