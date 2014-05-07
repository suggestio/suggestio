package util.img

import util.{FormUtil, PlayMacroLogsImpl}
import io.suggest.img.{ConvertModes, ImgCrop, SioImageUtilT}
import play.api.Play.current
import io.suggest.model.{MUserImgMetadata, MImgThumb, MUserImgOrig, MPict}
import scala.concurrent.{Future, future}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import java.io.{File, FileNotFoundException}
import org.apache.commons.io.FileUtils
import scala.util.{Failure, Success}
import java.lang
import com.fasterxml.jackson.annotation.JsonIgnore
import models._
import play.api.cache.Cache
import io.suggest.ym.model.common.MImgInfoT
import net.sf.jmimemagic.MagicMatch

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.02.14 15:14
 * Description: Для работы с загружаемыми картинками используются эти вспомогательные функции.
 */

object ImgFormUtil extends PlayMacroLogsImpl {
  import play.api.data.Forms._
  import play.api.data.Mapping
  import LOGGER._

  type LogoOpt_t = Option[ImgInfo4Save[ImgIdKey]]

  val IIK_MAXLEN = 80
  
  /** Маппер для поля с id картинки. Используется обертка над id чтобы прозрачно различать tmp и orig картинки. */
  val imgIdM: Mapping[ImgIdKey] = nonEmptyText(minLength = 8, maxLength = IIK_MAXLEN)
    .transform[ImgIdKey](ImgIdKey.apply, _.filename)
    .verifying("img.id.invalid.", { _.isValid })

  /** маппер для поля с id картинки, который может отсутствовать. */
  val imgIdOptM: Mapping[Option[ImgIdKey]] = optional(text(maxLength = IIK_MAXLEN))
    .transform[Option[ImgIdKey]](
       {txtOpt =>
         try {
           txtOpt
             .filter(_.length >= 8)
             .map(ImgIdKey.apply)
         } catch {
           case ex: Exception =>
             debug("imgIdOptM.apply: Cannot parse img id key: " + txtOpt, ex)
             None
         }
       },
       { _.map(_.filename) }
    )


  /** Маппер для поля с id картинки-логотипа, но результат конвертируется в ImgInfo. */
  def logoImgIdM(_imgIdM: Mapping[ImgIdKey]) = _imgIdM
    .transform(
      { ImgInfo4Save(_, cropOpt = None, withThumb = false) },
      { ii: ImgInfo4Save[ImgIdKey] => ii.iik }
    )

  /** Проверяем tmp-файл на использование jpeg. Уже сохраненные id не проверяются. */
  val imgIdJpegM = imgIdM
    .verifying("img.fmt.invalid", { iik => iik match {
      case tiik: TmpImgIdKey =>
        tiik.mptmp.data.fmt == OutImgFmts.JPEG
      case oiik: OrigImgIdKey =>
        true
    }})

  val LOGO_IMG_ID_K = "logoImgId"


  /** Синхронно проверить переданный img id key насколько это возможно. */
  def checkIIK(iik: ImgIdKey, marker: String): Boolean = {
    iik match {
      case tiik: TmpImgIdKey =>
        tiik.mptmp
          .data.markerOpt
          .exists(_ == marker)

      case _ => true
    }
  }

  /** Собрать маппинг для id изображения, промаркированного известным маркером, либо уже сохранённый orig. */
  def imgIdMarkedM(errorMsg: String, marker: String): Mapping[ImgIdKey] = {
    imgIdM.verifying(errorMsg, checkIIK(_, marker))
  }

  /** Аналог [[imgIdMarkedM]], но функция толерантна к ошибкам, и без ошибок отсеивает некорректные img id. */
  def imgIdMarkedOptM(marker: String): Mapping[Option[ImgIdKey]] = {
    imgIdOptM.transform[Option[ImgIdKey]](
      { _.filter(checkIIK(_, marker)) },
      { identity }
    )
  }

  /** Генератор logo-маппингов. */
  def getLogoKM(errorMsg: String, marker: String): (String, Mapping[LogoOpt_t]) = {
    val imgIdM = imgIdMarkedM(errorMsg, marker = marker)
    val logoImgInfoM = ImgFormUtil.logoImgIdM(imgIdM)
    LOGO_IMG_ID_K -> optional(logoImgInfoM)
  }

  /** Маппинг обязательного параметра кропа на реальность. */
  val imgCropM: Mapping[ImgCrop] = nonEmptyText(minLength = 4, maxLength = 16)
    .transform[Option[ImgCrop]] (ImgCrop.maybeApply, _.map(_.toCropStr).getOrElse(""))
    .verifying("crop.invalid", _.isDefined)
    .transform[ImgCrop](_.get, Some.apply)

  val imgCropOptM: Mapping[Option[ImgCrop]] = {
    val txtM = text(maxLength = 16).transform(FormUtil.strTrimSanitizeLowerF, FormUtil.strIdentityF)
    optional(txtM)
      .transform[Option[String]] (_.filter(!_.isEmpty), identity)
      .transform[Option[ImgCrop]] (_.flatMap(ImgCrop.maybeApply), _.map(_.toCropStr))
  }


  /** Нередко бывает несколько картинок при сабмите. */
  def mergeListMappings(iiks: List[ImgIdKey], iCrops: List[ImgCrop]): List[ImgInfo4Save[ImgIdKey]] = {
    iiks.zip(iCrops) map {
      case (iik, crop) => ImgInfo4Save(iik, Some(crop))
    }
  }

  def updateOrigImgId(needImg: Option[ImgInfo4Save[ImgIdKey]], oldImgId: Option[String]): Future[List[MImgInfo]] = {
    updateOrigImg(needImg, oldImgId.map(MImgInfo(_)))
  }
  
  /**
   * Замена иллюстрации к некоей сущности.
   * @param needImgs Необходимые в результате набор картинок. Тут могут быть как уже сохранённыя картинка,
   *                 так и новая из tmp. Если Nil, то старые картинки будут удалены.
   * @param oldImgs Уже сохранённые ранее картинки, если есть.
   * @return Список id новых и уже сохранённых картинок.
   */
  def updateOrigImg(needImgs: Option[ImgInfo4Save[ImgIdKey]], oldImgs: Option[MImgInfo]): Future[List[MImgInfo]] = {
    // TODO Эту фунцию можно быстро переделать с Option[] на Seq[]. Изначально она и работала через Seq. Но они не совместимы. Надо как-то это устаканить.
    val oldImgsSet = oldImgs.toSet
    val newTmpImgs = needImgs.iterator
      .filter { _.iik.isInstanceOf[TmpImgIdKey] }
      .map { _.asInstanceOf[ImgInfo4Save[TmpImgIdKey]] }
      .toList
    val needOrigImgs0 = needImgs.iterator
      .filter { _.iik.isInstanceOf[OrigImgIdKey] }
      .map { _.asInstanceOf[ImgInfo4Save[OrigImgIdKey]] }
    val needOrigImgs = oldImgsSet
      .filter { oii => needOrigImgs0.exists(_.iik == oii) } // Отбросить orig-картинки, которых не было среди старых оригиналов.
      .toList
    val delOldImgs = oldImgsSet -- needOrigImgs  // TODO Раньше были списки, теперь их нет. Надо убрать множество.
    // Запускаем в фоне удаление старых картинок. TODO Возможно, надо этот фьючерс подвязывать к фьючерсу сохранения?
    Future.traverse(delOldImgs) { oldOiik =>
      val fut = MPict.deleteFully(oldOiik.data.rowKey)
      fut onComplete {
        case Success(_)  => trace("Old img deleted: " + oldOiik)
        case Failure(ex) => error("Failed to delete old img " + oldOiik, ex)
      }
      fut
    }
    Future.traverse(newTmpImgs) { tii =>
      val fut = handleTmpImageForStoring(tii)
      fut onComplete { case tryResult =>
        tii.iik.mptmp.file.delete()
        tmpMetaCacheInvalidate(tii.iik)
      }
      fut onFailure {
        case ex =>  error(s"Failed to store picture " + tii, ex)
      }
      fut recover {
        case ex: Exception => null
      }
    } map { savedTmpImgsOrNull =>
      val newSavedIds = savedTmpImgsOrNull
        .filter { _ != null }
        .map { _.toMImgInfo }
      // TODO Нужно восстанавливать исходный порядок! Сейчас пока плевать на это, но надо это как-то исправлять.
      // Как вариант: можно уйти от порядка и от работы со списками картинок. А работать только с максимум одной картинкой через Option[] вместо Seq[].
      newSavedIds ++ needOrigImgs
    }
  }

  /**
   * В контроллер приходит сабмит по картинкам. Для tmp-картинок надо отправить их целиком в orig-хранилище.
   * Для любых картинок надо обновить нарезку, применив crop и отправив в хранилище.
   * @param tii Исходная tmp-картинка.
   * @return Фьючерс, содержащий imgId в виде строки.
   */
  def handleTmpImageForStoring(tii: ImgInfo4Save[TmpImgIdKey]): Future[SavedTmpImg] = {
    import tii.iik.mptmp
    val id = MPict.randomId
    val idStr = MPict.idBin2Str(id)
    // Запустить чтение из уже отрезайзенного tmp-файла и сохранение как-бы-исходного материала в HBase
    val saveOrigFut = future {
      OrigImageUtil.maybeReadFromFile(mptmp.file)
    } flatMap { imgBytes =>
      MUserImgOrig(idStr, imgBytes, q = tii.iik.origQualifier)
        .save
        .map { _ => mptmp.file -> idStr }
    }
    // 26.mar.2014: понадобился доступ к метаданным картинки в контексте элемента. Запускаем identify в фоне
    val imgMetaFut: Future[MImgInfoMeta] = future {
      OrigImageUtil.identify(mptmp.file)
    } flatMap { identifyResult =>
      // 2014.04.22: Сохранение метаданных в HBase для доступа в ad-preview.
      val savedMeta = Map(
        "w" -> identifyResult.getImageWidth.toString,
        "h" -> identifyResult.getImageHeight.toString
      )
      MUserImgMetadata(idStr, savedMeta).save map { _ =>
        MImgInfoMeta(
          height = identifyResult.getImageHeight,
          width  = identifyResult.getImageWidth
        )
      }
    }
    // Если укаазно withThumb, то пора сгенерить thumbnail без учёта кропов всяких.
    val saveThumbFut = if (tii.withThumb) {
      future {
        val tmpThumbFile = File.createTempFile("origThumb", ".jpeg")
        val thumbBytes = try {
          ThumbImageUtil.convert(mptmp.file, tmpThumbFile, mode = ConvertModes.THUMB, crop = tii.cropOpt)
          // Прочитать файл в памяти и сохранить в MImgThumb
          FileUtils.readFileToByteArray(tmpThumbFile)
        } finally {
          tmpThumbFile.delete()
        }
        val thumbDatum = new MImgThumb(id=id, thumb=thumbBytes, imageUrl=null)
        thumbDatum.save
      }
    } else {
      Future successful ()
    }
    // Связываем все асинхронные задачи воедино
    for {
      _ <- saveThumbFut
      (tmpFile, idStr) <- saveOrigFut
      imgMeta <- imgMetaFut
    } yield {
      SavedTmpImg(idStr, tmpFile, imgMeta)
    }
  }


  /**
   * Асинхронное удаление отработанных времененных файлов.
   * @param imgs Поюзанные картинки.
   * @return Фьючерс для синхронизации.
   */
  def rmTmpImgs(imgs: Seq[SavedTmpImg]): Future[_] = {
    future {
      imgs.foreach { img =>
        try {
          img.tmpImgFile.delete()
        } catch {
          case ex: FileNotFoundException =>
            trace("File already deleted: " + img.tmpImgFile.getAbsolutePath)
          case ex: Throwable => error("Failed to delete file " + img.tmpImgFile.getAbsolutePath, ex)
        }
      }
    }
  }

  /** Для временной картинки прочитать данные для постоения объекта метаданных. */
  def getMetaForTmpImg(img: TmpImgIdKey): Option[MImgInfoMeta] = {
    val info = OrigImageUtil.identify(img.mptmp.file)
    val result = MImgInfoMeta(height = info.getImageHeight, width = info.getImageWidth)
    Some(result)    // TODO Option -- тут аттавизм, пока остался, потом надо будет удалить.
  }

  def getTmpMetaCacheKey(id: String) = id + ".tme"
  val TMP_META_EXPIRE_SEC: Int = current.configuration.getInt("img.tmp.meta.cache.expire.seconds") getOrElse 40

  def getMetaForTmpImgCached(img: TmpImgIdKey): Option[MImgInfoMeta] = {
    val ck = getTmpMetaCacheKey(img.mptmp.data.key)
    Cache.getOrElse(ck, TMP_META_EXPIRE_SEC) {
      getMetaForTmpImg(img)
    }
  }

  def tmpMetaCacheInvalidate(tii: TmpImgIdKey) {
    val ck = getTmpMetaCacheKey(tii.filename)
    Cache.remove(ck)
  }


  /** Приведение выхлопа мапперов imgId к результату сохранения, минуя это самое сохранение. */
  implicit def logoOpt2imgInfo(logoOpt: LogoOpt_t): Option[MImgInfo] = {
    logoOpt.map { logo => MImgInfo(logo.iik.filename) }
  }

  /** Конвертер данных из готовых MImgInfo в [[OrigImgIdKey]]. */
  implicit def imgInfo2imgKey(m: MImgInfo): OrigImgIdKey = {
    OrigImgIdKey(m.filename, m.meta)()
  }
}


/** Резайзилка картинок, используемая для генерация "оригиналов", т.е. картинок, которые затем будут кадрироваться. */
object OrigImageUtil extends SioImageUtilT with PlayMacroLogsImpl {
  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  override def MAX_OUT_FILE_SIZE_BYTES: Option[Int] = None

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  override val MIN_SZ_PX: Int = current.configuration.getInt("img.orig.sz.min.px") getOrElse 256

  /** Если исходный jpeg после стрипа больше этого размера, то сделать resize.
    * Иначе попытаться стрипануть icc-профиль по jpegtran, чтобы снизить размер без пересжатия. */
  override def MAX_SOURCE_JPEG_NORSZ_BYTES: Option[Long] = None

  /** Качество сжатия jpeg. */
  override val JPEG_QUALITY_PC: Double = current.configuration.getDouble("img.orig.jpeg.quality") getOrElse 90.0

  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  override val DOWNSIZE_HORIZ_PX: Integer  = Integer valueOf (current.configuration.getInt("img.orig.maxsize.h.px") getOrElse 2048)
  override val DOWNSIZE_VERT_PX:  Integer  = current.configuration.getInt("img.orig.maxsize.v.px").map(Integer.valueOf) getOrElse DOWNSIZE_HORIZ_PX

  override val GAUSSIAN_BLUG: Option[lang.Double] = None
}


object ThumbImageUtil extends SioImageUtilT with PlayMacroLogsImpl {
  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  val DOWNSIZE_HORIZ_PX: Integer = Integer valueOf (current.configuration.getInt("img.thumb.maxsize.h.px") getOrElse 256)
  val DOWNSIZE_VERT_PX : Integer = current.configuration.getInt("img.thumb.maxsize.h.px").map(Integer.valueOf) getOrElse DOWNSIZE_HORIZ_PX

  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  override def MAX_OUT_FILE_SIZE_BYTES: Option[Int] = None

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  override def MIN_SZ_PX: Int = DOWNSIZE_HORIZ_PX.intValue()

  /** Если исходный jpeg после стрипа больше этого размера, то сделать resize.
    * Иначе попытаться стрипануть icc-профиль по jpegtran, чтобы снизить размер без пересжатия. */
  override def MAX_SOURCE_JPEG_NORSZ_BYTES: Option[Long] = None

  /** Качество сжатия jpeg. */
  override val JPEG_QUALITY_PC: Double = current.configuration.getDouble("img.thumb.jpeg.quality") getOrElse 85.0
}


/** Обработка логотипов. */
object AdnLogoImageUtil extends SioImageUtilT with PlayMacroLogsImpl {

  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  val DOWNSIZE_HORIZ_PX: Integer = Integer valueOf (current.configuration.getInt("img.logo.shop.maxsize.h.px") getOrElse 512)
  val DOWNSIZE_VERT_PX: Integer  = Integer valueOf (current.configuration.getInt("img.logo.shop.maxsize.v.px") getOrElse 128)

  /** Качество сжатия jpeg. */
  val JPEG_QUALITY_PC: Double = current.configuration.getDouble("img.logo.shop.jpeg.quality") getOrElse 0.95

  /** Если исходный jpeg после стрипа больше этого размера, то сделать resize.
    * Иначе попытаться стрипануть icc-профиль по jpegtran, чтобы снизить размер без пересжатия. */
  def MAX_SOURCE_JPEG_NORSZ_BYTES: Option[Long] = None

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  val MIN_SZ_PX: Int = current.configuration.getInt("img.logo.shop.side.min.px") getOrElse 30

  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  def MAX_OUT_FILE_SIZE_BYTES: Option[Int] = None
}


/** Там, где допустимы квалратные логотипы, используем этот трайт. */
trait SqLogoImageUtil  extends SioImageUtilT with PlayMacroLogsImpl {

  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  val DOWNSIZE_HORIZ_PX: Integer = Integer valueOf (current.configuration.getInt("img.logo.mart.maxsize.h.px") getOrElse 512)
  val DOWNSIZE_VERT_PX: Integer  = current.configuration.getInt("img.logo.mart.maxsize.v.px").map(Integer valueOf) getOrElse DOWNSIZE_HORIZ_PX

  /** Качество сжатия jpeg. */
  val JPEG_QUALITY_PC: Double = current.configuration.getDouble("img.logo.mart.jpeg.quality") getOrElse 0.95

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  val MIN_SZ_PX: Int = current.configuration.getInt("img.logo.mart.side.min.px") getOrElse 70

  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  val MAX_OUT_FILE_SIZE_BYTES: Option[Int] = current.configuration.getInt("img.logo.mart.result.size.max")

  /** Если исходный jpeg после стрипа больше этого размера, то сделать resize.
    * Иначе попытаться стрипануть icc-профиль по jpegtran, чтобы снизить размер без пересжатия. */
  def MAX_SOURCE_JPEG_NORSZ_BYTES: Option[Long] = None

}

/** Конвертор картинок в логотипы ТЦ. */
object MartLogoImageUtil extends SqLogoImageUtil

/** Конвертор картинок во вторичные логотипы на рекламных карточках. */
object AdLogoImageUtil extends SqLogoImageUtil


object ImgIdKey {
  def apply(key: String): ImgIdKey = {
    if (key startsWith MPictureTmp.KEY_PREFIX) {
      TmpImgIdKey(key)
    } else {
      OrigImgIdKey(key)()
    }
  }
}

sealed trait ImgIdKey {
  def filename: String
  def isExists: Future[Boolean]
  def isValid: Boolean
  def isTmp: Boolean
  override def hashCode = filename.hashCode
  def cropOpt: Option[ImgCrop]

  // Определение hbase qualifier для сохранения/чтения картинки по этому ключу.
  def origQualifierOpt = cropOpt.map { _.toCropStr }
  def origQualifier = cropOpt.fold(MPict.Q_USER_IMG_ORIG) { _.toCropStr }
}

case class TmpImgIdKey(filename: String) extends ImgIdKey {
  @JsonIgnore
  val mptmp: MPictureTmp = MPictureTmp(filename)

  @JsonIgnore
  override def cropOpt = mptmp.data.cropOpt

  @JsonIgnore
  override def isTmp: Boolean = true

  @JsonIgnore
  override def isExists: Future[Boolean] = Future successful isValid

  @JsonIgnore
  override def isValid: Boolean = mptmp.isExist
}


object OrigImgIdKey {
  import io.suggest.img.ImgUtilParsers._

  val FILENAME_PARSER: Parser[OrigImgData] = {
    "(?i)[0-9a-z_-]+".r ~ opt("~" ~> ImgCrop.CROP_STR_PARSER) ^^ {
      case rowKey ~ cropOpt =>
        OrigImgData(rowKey, cropOpt)
    }
  }

  def parseFilename(filename: String) = parse(FILENAME_PARSER, filename)
}

case class OrigImgData(rowKey: String, cropOpt: Option[ImgCrop]) {
  def toFilename: String = {
    var sbSz: Int = rowKey.length
    if (cropOpt.isDefined)
      sbSz += 22
    val sb = new StringBuilder(sbSz, rowKey)
    if (cropOpt.isDefined)
      sb.append('~').append(cropOpt.get.toUrlSafeStr)
    sb.toString()
  }
}

case class OrigImgIdKey(filename: String, meta: Option[MImgInfoMeta] = None)
                       (val data: OrigImgData = OrigImgIdKey.parseFilename(filename).get)
  extends MImgInfoT with ImgIdKey
{
  @JsonIgnore
  override def isTmp: Boolean = false

  @JsonIgnore
  override def isExists: Future[Boolean] = {
    MUserImgOrig.getById(filename, q = None).map(_.isDefined)
  }

  @JsonIgnore
  override def isValid: Boolean = MPict.isStrIdValid(filename)

  override def equals(obj: scala.Any): Boolean = {
    // Сравниваем с MImgInfo без учёта поля meta.
    obj match {
      case mii: MImgInfoT => (mii eq this) || (mii.filename == this.filename)
      case _ => false
    }
  }

  override def cropOpt = data.cropOpt
}


/** Выходные форматы картинок. */
object OutImgFmts extends Enumeration {
  type OutImgFmt = Value
  val JPEG = Value("jpeg")
  val PNG  = Value("png")
  val GIF  = Value("gif")

  /**
   * Предложить формат для mime-типа.
   * @param mime Строка mime-типа.
   * @return OutImgFmt. Если не-image тип, то будет IllegalArgumentException.
   */
  def forImageMime(mime: String): OutImgFmt = {
    if (mime equalsIgnoreCase "image/png") {
      PNG
    } else if (mime equalsIgnoreCase "image/gif") {
      GIF
    } else if (mime startsWith "image/") {
      JPEG
    } else {
      throw new IllegalArgumentException("Unknown/unsupported MIME type: " + mime)
    }
  }
}

import OutImgFmts._

/**
 * Класс для объединения кропа и id картинки (чтобы не использовать Tuple2 с числовыми названиями полей)
 * @param iik Указатель на картинку.
 * @param cropOpt Данные по желаемому кадрированию.
 * @tparam T Реальный тип iik.
 */
case class ImgInfo4Save[+T <: ImgIdKey](
  iik     : T,
  cropOpt : Option[ImgCrop] = None,
  withThumb: Boolean = true
)


case class SavedTmpImg(idStr:String, tmpImgFile:File, meta: MImgInfoMeta) {
  def toMImgInfo = MImgInfo(idStr, Some(meta))
}
