package util.img

import java.nio.ByteBuffer
import java.util.UUID

import io.suggest.util.UuidUtil
import util.{PlayLazyMacroLogsImpl, FormUtil, PlayMacroLogsImpl}
import io.suggest.img.{ConvertModes, ImgCrop, SioImageUtilT}
import play.api.Play.{current, configuration}
import io.suggest.model.{MUserImgMetadata, MUserImgOrig, MPict}
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
import play.api.Logger

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

  // Ключи в карте MUserImgMeta, которые хранят данные о картинке.
  val IMETA_WIDTH  = "w"
  val IMETA_HEIGHT = "h"

  /** Включение ревалидации уже сохраненных картинок при обновлении позволяет убирать картинки "дырки",
    * появившиеся в ходе ошибочной логики. */
  val REVALIDATE_ALREADY_SAVED_IMGS = configuration.getBoolean("img.update.revalidate.already.saved") getOrElse false


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
      { ImgInfo4Save(_, withThumb = false) },
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


  def updateOrigImgId(needImgs: Seq[ImgInfo4Save[ImgIdKey]], oldImgIds: Iterable[String]): Future[List[MImgInfoT]] = {
    updateOrigImgFull(needImgs, oldImgIds.map(MImgInfo(_)))
  }

  /** Комбо из updateOrigImgFull() и miiPreferFirstCropped(). */
  def updateOrigImg(needImgs: Seq[ImgInfo4Save[ImgIdKey]], oldImgs: Iterable[MImgInfoT]): Future[Option[MImgInfoT]] = {
    updateOrigImgFull(needImgs, oldImgs)
      .map { miiPreferFirstCropped }
  }

  /**
   * Замена иллюстраций к некоей сущности.
   * @param needImgs Необходимые в результате набор картинок. Тут могут быть как уже сохранённыя картинка,
   *                 так и новая из tmp. Если Nil, то старые картинки будут удалены.
   * @param oldImgs Уже сохранённые ранее картинки, если есть.
   * @return Список id новых и уже сохранённых картинок.
   */
  def updateOrigImgFull(needImgs: Seq[ImgInfo4Save[ImgIdKey]], oldImgs: Iterable[MImgInfoT]): Future[List[MImgInfoT]] = {
    // Защита от какой-либо деятельности в случае полного отсутствия входных данных.
    if (needImgs.isEmpty && oldImgs.isEmpty) {
      Future successful Nil
    } else {
      updateOrigImgFullDo(needImgs, oldImgs = oldImgs)
    }
  }
  private def updateOrigImgFullDo(needImgs: Seq[ImgInfo4Save[ImgIdKey]], oldImgs: Iterable[MImgInfoT]): Future[List[MImgInfoT]] = {
    // newTmpImgs - это одноразовый итератор, содержит исходные индексы и списки картинок для сохранения.
    val newTmpImgs = needImgs
      .iterator
      .zipWithIndex
      // В новых (произведённых) картинках есть только добавленные картинки.
      .filter { case (ii4s, i)  =>  ii4s.iik.isInstanceOf[TmpImgIdKey] }
      // 2014.05.08: Нужно сохранять ещё и исходную tmp-картинку, если передана откадрированная tmp-картинка.
      .map { case (ii4sRaw, i) =>
        val ti4s = ii4sRaw.asInstanceOf[ImgInfo4Save[TmpImgIdKey]]
        val id = UUID.randomUUID()
        val idStr = UuidUtil.uuidToBase64(id)
        val idOpt = Some(idStr)
        val results = if (ti4s.iik.isCropped) {
          // Это откадрированная картинка, значит рядом лежит оригинал. Надо срезать crop и тоже схоронить.
          List(
            ti4s.copy(withId = idOpt),
            // Восстанавливать связи между исходными orig-картинками и need tmp пока нет удобной возможности, поэтому тут по сути идёт пересохранение orig-картинки под новым id с параллельным удаление старой.
            // TODO Надо сделать так, чтобы откадрированные оригиналы сохранялись под исходными id, при этом исходники не пересохранялись.
            ti4s.copy(iik = ti4s.iik.uncropped, withId = idOpt, withDownsize = None)
          )
        } else {
          // Это картинка без кадрирования. Скорее всего она деформирована, но суть одна - надо бы сохранить оригинал про запас.
          if (ti4s.withDownsize.isDefined) {
            // Нужно сохранить ~оригинал без downsize
            List(
              ti4s.copy(withId = idOpt),
              ti4s.copy(withId = idOpt, withDownsize = None)
            )
          } else {
            // downsize отключён - в топку
            List(ti4s)
          }
        }
        results -> i
      }
    // Запустить сохранение tmp-картинок.
    val savedTmpImgsFut = Future.traverse(newTmpImgs) { case (tiis, i) =>
      Future.traverse(tiis) { tii =>
        val fut = handleTmpImageForStoring(tii)
          .map { sti => Some(sti -> i) }
        fut onComplete { case tryResult =>
          tii.iik.mptmp.file.delete()
          tmpMetaCacheInvalidate(tii.iik)
        }
        fut recover {
          case ex: Exception =>
            error(s"Failed to store picture " + tii, ex)
            None
        }
      }
    } map { savedTmpImgsOrNull =>
      // Строим результат сохранения новых tmp-картинок.
      savedTmpImgsOrNull
        .flatten // Разворачиваем list'ы сохранённых tmp-картинок в разных размерах
        .flatten // Развернуть Option'ы, которые подавляют ошибки сохранения.
        .map { case (savedImg, i) => savedImg.toMImgInfo -> i}
        .toList
    }
    // Какие картинки надо оставить с прошлого раза и отфорвардить в результаты
    val needOrigImgs1 = needImgs
      .iterator
      .zipWithIndex
      .filter { case (ii4s, i) => ii4s.iik.isInstanceOf[OrigImgIdKey] }
      // Из старых картинок выбрать подходящую уже сохранённую, если она там есть.
      .flatMap { case (ti4s, i) => oldImgs.find { oii => ti4s.iik == oii }.map { _ -> i } }
      .toList

    // 2014.09.18: Из-за бага с удалением ненужных картинок, была введена валидация уже сохранённых orig img, подлежащих повторному сохранению.
    // Это нужно будет отключить, когда "дырки" в галереях карточек и узлов исчезнут. Валидация уже сохранённых картинок сильно замедляет сохранение.
    val needOrigImgsFilteredFut = if (REVALIDATE_ALREADY_SAVED_IMGS) {
      Future.traverse(needOrigImgs1) { case v @ (miit, _) =>
        try {
          val oid: OrigImgIdKey = miit
          MUserImgMeta2.getByStrId(oid.data.rowKey, oid.origQualifierOpt)
            .map {
              // Всё ок, картинка скорее всего есть в базе, пропускаем.
              case Some(_) =>
                Some(v)
              // Ревалидация выявила проблему: картинки нет в базе. Затираем весь ряд для надежности.
              case None =>
                eraseOiik(oid)
                warn("Revalidate: Found invalid orig img reference: " + oid + " -- Reerasing it and forgetting.")
                None
            }
        } catch {
          case ex: Exception => Future successful None
        }
      } map {
        _.flatMap(identity(_))
      }
    } else {
      Future successful needOrigImgs1
    }

    // Восстановить исходный порядок needImgs на основе исходных индексов, собрать финальный результат метода.
    val resultFut = for {
      newSavedImgs  <- savedTmpImgsFut
      needOrigImgs2 <- needOrigImgsFilteredFut
    } yield {
      // 2014.sep.19: Бывает ситуация, когда картинка была сохранена оригиналом, а потом откропана и снова сохранена под тем же rowkey. Нужно устранять дубликаты.
      val needOrigImgs3 = needOrigImgs2
        .filter { case (miit, _) =>
          val oiit: OrigImgIdKey = miit
          !newSavedImgs.exists {
            case (miitNew, _) =>
              miitNew.data.rowKey == oiit.data.rowKey
          }
        }
      (newSavedImgs ++ needOrigImgs3)
        // Вышеуказанная проблема всё равно присутствует...
        .groupBy(_._1.data.rowKey)
        // Отфильтровываем orig-картинку при наличии кропаной версии.
        .mapValues(_.sortBy(_._1.filename).reverse.head)
        .values
        .toList
        .sortBy(_._2)
        .map(_._1)
    }
    // Если сохранение удалось, то надо запустить в фоне удаление старых картинок.
    resultFut onSuccess { case _ =>
      needOrigImgsFilteredFut onSuccess { case needOrigImgs2 =>
        // 2014.sep.18: Нужно удалять по ключам и сравнивать по ключам, без учёта кропов всяких и т.д.
        val delOldImgs = oldImgs.filterNot { miit =>
          val oiik: OrigImgIdKey = miit
          needOrigImgs2.exists { needOI =>
            needOI._1.data.rowKey  ==  oiik.data.rowKey
          }
        }
        if (delOldImgs.nonEmpty)
          debug("Will delete unused imgs: " + delOldImgs.mkString(", "))
        Future.traverse(delOldImgs)(eraseOiik)
      }
    }
    // Вернуть результат.
    resultFut
  }


  private def eraseOiik(oiik: MImgInfoT): Future[_] = {
    val rowKey = oiik.data.rowKey
    val fut = Future.traverse(Seq(MUserImg2, MUserImgMeta2, MImgThumb2)) { imodel =>
      imodel.deleteByStrId(rowKey)
    }
    fut onComplete {
      case Success(res) => trace("Old img deleted: " + oiik + " result: " + res)
      case Failure(ex)  => error("Failed to delete old img " + oiik, ex)
    }
    fut
  }

  /**
   * В контроллер приходит сабмит по картинкам. Для tmp-картинок надо отправить их целиком в orig-хранилище.
   * Для любых картинок надо обновить нарезку, применив crop и отправив в хранилище.
   * @param tii Исходная tmp-картинка.
   * @return Фьючерс, содержащий imgId в виде строки.
   */
  def handleTmpImageForStoring(tii: ImgInfo4Save[TmpImgIdKey]): Future[SavedTmpImg] = {
    import tii.iik.mptmp
    lazy val logPrefix = s"handleTmpImageForStoring(${tii.iik.filename}): "
    trace(s"${logPrefix}Starting: crop=${tii.iik.cropOpt}, withThumb=${tii.withThumb} withId=${tii.withId}")
    val (rowkeyStr, rowkey) = tii.withId match {
      case Some(_rowkeyStr) =>
        _rowkeyStr -> UuidUtil.base64ToUuid(_rowkeyStr)
      case None =>
        val _rowkey = UUID.randomUUID()
        val _rowkeyStr = UuidUtil.uuidToBase64(_rowkey)
        _rowkeyStr -> _rowkey
    }
    // 26.sep.2014: Костыли для поддержки svg.
    val isVectorImage = mptmp.file.getName.endsWith(".svg")
    // qualifier для сохранения картинки и её метаданных
    // Запустить чтение из уже отрезайзенного tmp-файла и сохранение как-бы-исходного материала в модель.
    val mptmp4save: MPictureTmp = tii.withDownsize
      .filter { _ => !isVectorImage }   // svg не имеют фактического размера, делать downsize нельзя.
      .fold(mptmp) { iim =>
        val dsImgUtil = DownsizeImageUtil(iim)
        val mptmpNew = MPictureTmp.mkNew(cropOpt = tii.iik.cropOpt, outFmt = OutImgFmts.JPEG)
        dsImgUtil.convert(fileOld = mptmp.file, fileNew = mptmpNew.file)
        mptmpNew
      }
    // Запускаем сборку данных по финальной сохраняемой картинке
    // 26.sep.2014: TODO svg-картинки не имеют размера, но identify как-то его определяет.
    val identifyResult = OrigImageUtil.identify(mptmp4save.file)
    // Параметр кропа отображаем на выходной размер фотки.
    val crop4nameOpt: Option[ImgCrop] = if (isVectorImage) {
      None
    } else {
      val crop4name0 = tii.iik.cropOpt
      tii.withDownsize.fold { crop4name0 } { wds =>
        crop4name0.map {
          _.copy(h = identifyResult.getImageHeight, w = identifyResult.getImageWidth)
        } orElse {
          // Кропа нема, но задан downsize. надо бы указать в имени, что что-то типа кропа присутствует.
          val pseudoCrop = ImgCrop(h = identifyResult.getImageHeight, w = identifyResult.getImageWidth, offX = 0, offY = 0)
          Some(pseudoCrop)
        }
      }
    }
    val q = ImgIdKey.origQualifier(crop4nameOpt)
    val saveOrigFut = future {
      OrigImageUtil.maybeReadFromFile(mptmp4save.file)
    } flatMap { imgBytes =>
      MUserImg2(id = rowkey, img = ByteBuffer.wrap(imgBytes), q = q)
        .save
        .map { _ =>
          val savedFilename = OrigImgData(rowkeyStr, cropOpt = crop4nameOpt).toFilename
          mptmp4save.file -> savedFilename
        }
    }
    saveOrigFut onComplete {
      case Success(result) => trace(logPrefix + "Orig img saved ok. Result = " + result)
      case Failure(ex)     => error(logPrefix + "Failed to save img.", ex)
    }
    // 26.mar.2014: понадобился доступ к метаданным картинки в контексте элемента. Запускаем identify в фоне
    val imgMetaFut: Future[MImgInfoMeta] = {
      // 2014.04.22: Сохранение метаданных в HBase для доступа в ad-preview.
      val w = identifyResult.getImageWidth
      val h = identifyResult.getImageHeight
      val savedMeta = Map(
        IMETA_WIDTH  -> w.toString,
        IMETA_HEIGHT -> h.toString
      )
      MUserImgMeta2(id = rowkey, md = savedMeta, q = q)
        .save
        .map { _ => MImgInfoMeta(height = h,  width = w) }
    }
    imgMetaFut onComplete {
      case Success(result) => trace(logPrefix + "Img metadata saved ok: " + result)
      case Failure(ex)     => error(logPrefix + "Failed to save img metadata.", ex)
    }
    // Если указано withThumb, то пора сгенерить thumbnail без учёта кропов всяких.
    val saveThumbFut = if (tii.withThumb) {
      val stFut = future {
        val tmpThumbFile = File.createTempFile("origThumb", ".jpeg")
        val thumbBytes = try {
          ThumbImageUtil.convert(mptmp.file, tmpThumbFile, mode = ConvertModes.THUMB)
          // Прочитать файл в памяти и сохранить в MImgThumb
          FileUtils.readFileToByteArray(tmpThumbFile)
        } finally {
          tmpThumbFile.delete()
        }
        new MImgThumb2(
          id = rowkey,
          img = ByteBuffer.wrap(thumbBytes),
          imageUrl = None
        )
      } flatMap { thumbDatum =>
        thumbDatum.save
      }
      stFut onComplete {
        case Success(_)  => trace(logPrefix + "Img thumb saved ok")
        case Failure(ex) => error(logPrefix + "Failed to save img thumbnail", ex)
      }
      stFut
    } else {
      Future successful ()
    }
    // Связываем все асинхронные задачи воедино
    for {
      _ <- saveThumbFut
      (tmpFile, idStr) <- saveOrigFut
      imgMeta <- imgMetaFut
    } yield {
      SavedTmpImg(idStr, tmpFile, imgMeta, tii.iik.cropOpt)
    }
  }


  /**
   * Т.к. updateOrigImg() возвращает список результатов, хотя подразумевается только один, то надо
   * выделять нужную картинку из выхлопа сохраненных картинок.
   * Так, при кадрировании возвращается две сохранённые картинки (исходная и кадрированная) вместо одной или нуля.
   * @param l Выхлоп updateOrigImg()
   * @return Опционально выбранная картинка.
   */
  def miiPreferFirstCropped(l: List[MImgInfoT]): Option[MImgInfoT] = {
    l.reduceOption { (mii1, mii2) =>
      val oiik1: OrigImgIdKey = mii1
      val oiik2: OrigImgIdKey = mii2
      (oiik1.isCropped, oiik2.isCropped) match {
        case (true, false) =>
          mii1
        case (false, true) =>
          mii2
        // Внезапно обе откропленные картинки. Выбираем ту, что имеет название по-длинее
        case _ =>
          trace(s"miiPreferFirstCropped($l): reduce(): Both imgs are cropped/uncropped. Selecting one with longest filename...")
          List(oiik1, oiik2)
            .map { oiik => oiik.filename -> oiik }
            .sortBy(_._1)
            .last
            ._2
      }
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
  val TMP_META_EXPIRE_SEC: Int = configuration.getInt("img.tmp.meta.cache.expire.seconds") getOrElse 40

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
  implicit def imgInfo2imgKey(m: MImgInfoT): OrigImgIdKey = {
    m match {
      case oiik: OrigImgIdKey =>
        oiik
      case _ =>
        OrigImgIdKey(m.filename, m.meta)
    }
  }
}


/** Резайзилка картинок, используемая для генерация "оригиналов", т.е. картинок, которые затем будут кадрироваться. */
object OrigImageUtil extends SioImageUtilT with PlayMacroLogsImpl {
  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  override def MAX_OUT_FILE_SIZE_BYTES: Option[Int] = None

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  override val MIN_SZ_PX: Int = configuration.getInt("img.orig.sz.min.px") getOrElse 256

  /** Если исходный jpeg после стрипа больше этого размера, то сделать resize.
    * Иначе попытаться стрипануть icc-профиль по jpegtran, чтобы снизить размер без пересжатия. */
  override def MAX_SOURCE_JPEG_NORSZ_BYTES: Option[Long] = None

  /** Качество сжатия jpeg. */
  override val JPEG_QUALITY_PC: Double = configuration.getDouble("img.orig.jpeg.quality") getOrElse 90.0

  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  override val DOWNSIZE_HORIZ_PX: Integer  = Integer valueOf (configuration.getInt("img.orig.maxsize.h.px") getOrElse 2048)
  override val DOWNSIZE_VERT_PX:  Integer  = configuration.getInt("img.orig.maxsize.v.px").map(Integer.valueOf) getOrElse DOWNSIZE_HORIZ_PX

  override def GAUSSIAN_BLUG: Option[lang.Double] = None
}


object ThumbImageUtil extends SioImageUtilT with PlayMacroLogsImpl {
  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  val DOWNSIZE_HORIZ_PX: Integer = Integer valueOf (configuration.getInt("img.thumb.maxsize.h.px") getOrElse 256)
  val DOWNSIZE_VERT_PX : Integer = configuration.getInt("img.thumb.maxsize.h.px").map(Integer.valueOf) getOrElse DOWNSIZE_HORIZ_PX

  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  override def MAX_OUT_FILE_SIZE_BYTES: Option[Int] = None

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  override def MIN_SZ_PX: Int = DOWNSIZE_HORIZ_PX.intValue()

  /** Если исходный jpeg после стрипа больше этого размера, то сделать resize.
    * Иначе попытаться стрипануть icc-профиль по jpegtran, чтобы снизить размер без пересжатия. */
  override def MAX_SOURCE_JPEG_NORSZ_BYTES: Option[Long] = None

  /** Качество сжатия jpeg. */
  override val JPEG_QUALITY_PC: Double = configuration.getDouble("img.thumb.jpeg.quality") getOrElse 85.0
}


/** Обработка логотипов. */
object AdnLogoImageUtil extends SioImageUtilT with PlayMacroLogsImpl {

  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  val DOWNSIZE_HORIZ_PX: Integer = Integer valueOf (configuration.getInt("img.logo.shop.maxsize.h.px") getOrElse 512)
  val DOWNSIZE_VERT_PX: Integer  = Integer valueOf (configuration.getInt("img.logo.shop.maxsize.v.px") getOrElse 128)

  /** Качество сжатия jpeg. */
  val JPEG_QUALITY_PC: Double = configuration.getDouble("img.logo.shop.jpeg.quality") getOrElse 0.95

  /** Если исходный jpeg после стрипа больше этого размера, то сделать resize.
    * Иначе попытаться стрипануть icc-профиль по jpegtran, чтобы снизить размер без пересжатия. */
  def MAX_SOURCE_JPEG_NORSZ_BYTES: Option[Long] = None

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  val MIN_SZ_PX: Int = configuration.getInt("img.logo.shop.side.min.px") getOrElse 30

  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  def MAX_OUT_FILE_SIZE_BYTES: Option[Int] = None
}


/** Там, где допустимы квалратные логотипы, используем этот трайт. */
trait SqLogoImageUtil  extends SioImageUtilT with PlayMacroLogsImpl {

  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  val DOWNSIZE_HORIZ_PX: Integer = Integer valueOf (configuration.getInt("img.logo.mart.maxsize.h.px") getOrElse 512)
  val DOWNSIZE_VERT_PX: Integer  = configuration.getInt("img.logo.mart.maxsize.v.px").map(Integer valueOf) getOrElse DOWNSIZE_HORIZ_PX

  /** Качество сжатия jpeg. */
  val JPEG_QUALITY_PC: Double = configuration.getDouble("img.logo.mart.jpeg.quality") getOrElse 0.95

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  val MIN_SZ_PX: Int = configuration.getInt("img.logo.mart.side.min.px") getOrElse 70

  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  val MAX_OUT_FILE_SIZE_BYTES: Option[Int] = configuration.getInt("img.logo.mart.result.size.max")

  /** Если исходный jpeg после стрипа больше этого размера, то сделать resize.
    * Иначе попытаться стрипануть icc-профиль по jpegtran, чтобы снизить размер без пересжатия. */
  def MAX_SOURCE_JPEG_NORSZ_BYTES: Option[Long] = None

}

/** Внутренняя экранизация sio image util для нужд ресайза с переменным размером. */
sealed case class DownsizeImageUtil(iim: MImgInfoMeta) extends SioImageUtilT with PlayLazyMacroLogsImpl {
  override def DOWNSIZE_VERT_PX: Integer = iim.height
  override def DOWNSIZE_HORIZ_PX: Integer = iim.width
  override def MAX_OUT_FILE_SIZE_BYTES = None
  override def MAX_SOURCE_JPEG_NORSZ_BYTES = None
  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  override def MIN_SZ_PX: Int = 256
  override def JPEG_QUALITY_PC: Double = 0.95
}


/** Конвертор картинок в логотипы ТЦ. */
object LogoImageUtil extends SqLogoImageUtil

/** Конвертор картинок во вторичные логотипы на рекламных карточках. */
object AdLogoImageUtil extends SqLogoImageUtil


object ImgIdKey {
  def apply(key: String): ImgIdKey = {
    if (key startsWith MPictureTmp.KEY_PREFIX) {
      TmpImgIdKey(key)
    } else {
      OrigImgIdKey(key)
    }
  }

  def origQualifier(cropOpt: Option[ImgCrop]) = cropOpt.fold(MPict.Q_USER_IMG_ORIG) { _.toCropStr }
}

sealed trait ImgIdKey {
  def filename: String
  def isExists: Future[Boolean]
  def isValid: Boolean
  def isTmp: Boolean
  override def hashCode = filename.hashCode
  def cropOpt: Option[ImgCrop]
  def getBaseImageWH: Future[Option[MImgInfoMeta]]
  def getImageWH: Future[Option[MImgInfoMeta]]

  // Определение hbase qualifier для сохранения/чтения картинки по этому ключу.
  def origQualifierOpt = cropOpt.map { _.toCropStr }
  def origQualifier = ImgIdKey.origQualifier(cropOpt)

  /** Является ли эта картинка кадрированной производной?
    * @return false, если картинка оригинальная. true если откадрированная картинка.
    */
  def isCropped = cropOpt.isDefined

  def uncropped: ImgIdKey
}


object TmpImgIdKey {
  def apply(filename: String): TmpImgIdKey = {
    val mptmp = MPictureTmp(filename)
    TmpImgIdKey(filename, mptmp)
  }
  def apply(mptmp: MPictureTmp): TmpImgIdKey = {
    val filename = mptmp.filename
    TmpImgIdKey(filename, mptmp)
  }
}

case class TmpImgIdKey(filename: String, @JsonIgnore mptmp: MPictureTmp) extends ImgIdKey {

  @JsonIgnore
  override def cropOpt = mptmp.data.cropOpt

  @JsonIgnore
  override def isTmp: Boolean = true

  @JsonIgnore
  override def isExists: Future[Boolean] = Future successful isValid

  @JsonIgnore
  override def isValid: Boolean = mptmp.isExist

  @JsonIgnore
  override def getBaseImageWH: Future[Option[MImgInfoMeta]] = {
    // TODO Нужно считать размер для неоткропанной картинки, если текущая уже откропана.
    getImageWH
  }

  override def getImageWH: Future[Option[MImgInfoMeta]] = {
    val result = try {
      val info = OrigImageUtil.identify(mptmp.file)
      val imeta = MImgInfoMeta(
        height = info.getImageHeight,
        width = info.getImageWidth
      )
      Some(imeta)
    } catch {
      case ex: org.im4java.core.InfoException =>
        Logger(getClass).info("getImageWH(): Unable to identity image " + filename, ex)
        None
    }
    Future successful result
  }

  @JsonIgnore
  override def uncropped: TmpImgIdKey = {
    if (isCropped) {
      val mptmpData1 = mptmp.data.copy(cropOpt = None)
      val mptmp1 = MPictureTmp(mptmpData1)
      TmpImgIdKey(mptmp1)
    } else {
      this
    }
  }
}


object OrigImgIdKey {
  import io.suggest.img.ImgUtilParsers._
  import ImgFormUtil.{IMETA_HEIGHT, IMETA_WIDTH}

  val FILENAME_PARSER: Parser[OrigImgData] = {
    "(?i)[0-9a-z_-]+".r ~ opt("~" ~> ImgCrop.CROP_STR_PARSER) ^^ {
      case rowKey ~ cropOpt =>
        OrigImgData(rowKey, cropOpt)
    }
  }

  def parseFilename(filename: String) = parse(FILENAME_PARSER, filename)

  def apply(filename: String): OrigImgIdKey = {
    apply(filename, meta = None)
  }
  def apply(filename: String, meta: Option[MImgInfoMeta]): OrigImgIdKey = {
    val data = OrigImgIdKey.parseFilename(filename).get
    apply(filename, meta, data)
  }
  def apply(data: OrigImgData): OrigImgIdKey = {
    apply(data, meta = None)
  }
  def apply(data: OrigImgData, meta: Option[MImgInfoMeta]): OrigImgIdKey = {
    val filename = data.toFilename
    apply(filename, meta, data)
  }
  def apply(filename: String, data: OrigImgData): OrigImgIdKey = {
    apply(filename, meta = None, data = data)
  }


  /** Прочитать ширину-длину хранимой orig-картинки по модели MUserImgMetadata.
    * Метод довольно статичен, но private чтобы не допускать логических ошибок при передаче параметров
    * (ведь можно ошибочно передать [[OrigImgIdKey.filename]] например -- функция будет вести себя ошибочно при crop).
    * @param rowKey Чистый ключ картинки. Доступен через [[OrigImgIdKey.data.rowKey]].
    * @return Асинхронные метаданные по ширине-высоте картинки.
    */
  private def getOrigImageWH(rowKey: String, qOpt: Option[String] = None): Future[Option[MImgInfoMeta]] = {
    MUserImgMeta2.getByStrId(rowKey, qOpt)
      .map { imetaOpt =>
        for {
          imeta     <- imetaOpt
          widthStr  <- imeta.md.get(IMETA_WIDTH)
          heightStr <- imeta.md.get(IMETA_HEIGHT)
        } yield {
          MImgInfoMeta(height = heightStr.toInt, width = widthStr.toInt)
        }
      }
      // TODO Можно попытаться прочитать картинку из хранилища, если метаданные по картинке не найдены.
  }

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

case class OrigImgIdKey(filename: String, meta: Option[MImgInfoMeta], data: OrigImgData)
  extends MImgInfoT with ImgIdKey
{
  @JsonIgnore
  override def isTmp: Boolean = false

  @JsonIgnore
  override def isExists: Future[Boolean] = {
    MUserImg2.getByStrId(data.rowKey, q = None).map(_.isDefined)
  }

  @JsonIgnore
  override def isValid: Boolean = UuidUtil.isUuidStrValid(filename)

  @JsonIgnore
  override def equals(obj: scala.Any): Boolean = {
    // Сравниваем с MImgInfo без учёта поля meta.
    obj match {
      case mii: MImgInfoT => (mii eq this) || (mii.filename == this.filename)
      case _ => false
    }
  }

  @JsonIgnore
  override def cropOpt = data.cropOpt

  @JsonIgnore
  override def getBaseImageWH = OrigImgIdKey.getOrigImageWH(data.rowKey)

  override def getImageWH = OrigImgIdKey.getOrigImageWH(data.rowKey, origQualifierOpt)

  override def uncropped: OrigImgIdKey = {
    if (isCropped) {
      val data1 = data.copy(cropOpt = None)
      val fn1 = data1.toFilename
      OrigImgIdKey(fn1, meta, data1)
    } else {
      this
    }
  }
}


/** Выходные форматы картинок. */
object OutImgFmts extends Enumeration {
  type OutImgFmt = Value
  val JPEG = Value("jpeg")
  val PNG  = Value("png")
  val GIF  = Value("gif")
  val SVG  = Value("svg")

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


/**
 * Класс для объединения кропа и id картинки (чтобы не использовать Tuple2 с числовыми названиями полей)
 * @param iik Указатель на картинку.
 * @param withThumb Генерить ли превьюшку? (она будет сохранена рядом).
 * @param withId Использовать указанный id. Если None, то будет сгенерен новый рандомный id.
 * @param withDownsize Дополнительно уменьшить картинку вниз перед сохранением. Нужно для того, чтобы не сохранять
 *                     большую картинку для маленького блока.
 * @tparam T Реальный тип iik.
 */
case class ImgInfo4Save[+T <: ImgIdKey](
  iik       : T,
  withThumb : Boolean = true,
  withId    : Option[String] = None,
  withDownsize : Option[MImgInfoMeta] = None
)


case class SavedTmpImg(idStr:String, tmpImgFile:File, meta: MImgInfoMeta, cropOpt: Option[ImgCrop] = None) {
  def toMImgInfo = MImgInfo(idStr, Some(meta))
}
