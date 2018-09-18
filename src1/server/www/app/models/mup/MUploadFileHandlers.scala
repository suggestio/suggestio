package models.mup

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumJvmUtil
import japgolly.univeq.UnivEq
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.10.17 10:22
  * Description: URL-qs-модель вариантов сохранения принимаемого файла на диск.
  */
object MUploadFileHandlers extends StringEnum[MUploadFileHandler] {

  /** Сохранять файл-картинку сразу в иерархию файлов MLocalImg. */
  case object Picture extends MUploadFileHandler("i")

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

}
