package models.mup

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumJvmUtil
import japgolly.univeq.UnivEq
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{MultipartFormData, QueryStringBindable}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.10.17 10:22
  * Description: URL-qs-модель вариантов сохранения принимаемого файла на диск.
  */
object MUploadFileHandlers extends StringEnum[MUploadFileHandler] {

  /** Сохранять файл-картинку сразу в иерархию файлов MLocalImg. */
  case object Image extends MUploadFileHandler("i")

  override val values = findValues

}


/** Класс одного элемента модели [[MUploadFileHandlers]]. */
sealed abstract class MUploadFileHandler(override val value: String) extends StringEnumEntry

/** Статическая поддержка элементов модели. */
object MUploadFileHandler {

  /** Поддержка биндинга из URL qs. */
  implicit def qsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MUploadFileHandler] = {
    EnumeratumJvmUtil.valueEnumQsb( MUploadFileHandlers )
  }


  /** Поддержка UnivEq. */
  @inline implicit def univEq: UnivEq[MUploadFileHandler] = UnivEq.derive


  /** Доп.API для [[MUploadFileHandler]]. */
  implicit final class UfhOpsExt( private val ufh: MUploadFileHandler ) extends AnyVal {

    /** Надо ли оставлять на диске в [[models.im.MLocalImg]] файлы, загруженные с указанным обработчиком. */
    def isKeepUploadedFile: Boolean = {
      ufh match {
        case MUploadFileHandlers.Image => true
        case _ => false
      }
    }

  }


  implicit final class UfhOptOpsExt( private val ufhOpt: Option[MUploadFileHandler] ) extends AnyVal {

    def isKeepUploadedFile: Boolean =
      ufhOpt.fold(false)( _.isKeepUploadedFile )

  }

}


/** Контейнер результата работы BodyParser'а при аплоаде. */
final case class MUploadBpRes(
                               data                     : MultipartFormData[TemporaryFile],
                               fileCreator              : LocalImgFileCreator,
                             )
