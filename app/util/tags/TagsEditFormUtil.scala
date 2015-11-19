package util.tags

import com.google.inject.Singleton
import models.mtag.{MTagsAddFormBinded, MTagBinded, TagsEditForm_t}
import play.api.data.{Form, Mapping}
import play.api.data.Forms._
import util.FormUtil
import io.suggest.common.tags.edit.TagsEditConstants._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.11.15 16:19
  * Description: Утиль для формы редактора связанных тегов.
  */
@Singleton
class TagsEditFormUtil {

  // TODO Оставлено для DI-реализации FormUtil. Может однажды руки дойдут наконец.
  private def formUtil = FormUtil

  /** Максимальная символьная длина одного тега. */
  def TAG_LEN_MAX = 40

  /** Минимальная символьная длина одного тега. */
  def TAG_LEN_MIN = 1

  /** Сколько тегов юзер может добавить за один запрос максимум. */
  def TAGS_PER_ADD_MAX = 20

  /** причёсывание имени тега. */
  private def _tagNamePrepare(raw: String): String = {
    // Регистр сохраняем исходный. Это нужно для FTS-токенизации в случае склееных слов.
    formUtil.stripHtml(raw)
      .replaceAll("(?U)([^\\w\\s]|_)", " ")
      // Убрать двойные пробелы, табуляции и т.д.
      .replaceAll("\\s+", " ")
      .trim()
  }

  private def _tagNamePrepareM(m0: Mapping[String]): Mapping[String] = {
    m0.transform[String](
      // Срезать знаки препинания, все \s+ заменить на одиночные пробелы.
      _tagNamePrepare, formUtil.strIdentityF
    )
    .verifying("e.tag.len.max", _.length <= TAG_LEN_MAX)
    .verifying("e.tag.len.min", _.length >= TAG_LEN_MIN)
  }

  def tagNameM: Mapping[String] = {
    val m0 = nonEmptyText(
      minLength = TAG_LEN_MIN,
      maxLength = TAG_LEN_MAX * 2
    )
    _tagNamePrepareM(m0)
  }


  /** Маппинг для строки, в которой может быть задано сразу несколько тегов. */
  def addedM: Mapping[Seq[String]] = {
    nonEmptyText(minLength = TAG_LEN_MIN, maxLength = 256)
      .transform [Seq[String]] (
        {allRaw =>
          val lenMin = TAG_LEN_MIN
          val lenMax = TAG_LEN_MAX
          allRaw.split("[,;|]")
            .iterator
            .map { _tagNamePrepare }
            .filter(_.length >= lenMin)
            .filter(_.length <= lenMax)
            .toSeq
        },
        _.mkString(", ")
      )
      .verifying("error.required", _.nonEmpty)
      .verifying("error.too.many.tags", _.size <= TAGS_PER_ADD_MAX)
  }

  /** Маппинг для данных по уже связанному тегу. */
  def existTagM: Mapping[MTagBinded] = {
    mapping(
      EXIST_TAG_NAME_FN -> tagNameM,
      EXIST_TAG_ID_FN   -> optional( formUtil.esIdM )
    )
    { MTagBinded.apply }
    { MTagBinded.unapply }
  }

  /** Маппинг для множественных значений поля тегов. */
  def existingsM: Mapping[List[MTagBinded]] = {
    list(existTagM)
  }


  def addedKm     = ADD_TAGS_FN   -> addedM
  def existingKm  = EXIST_TAGS_FN -> existingsM

  def addTagsFormM: Mapping[MTagsAddFormBinded] = {
    mapping(
      addedKm,
      existingKm
    )
    { MTagsAddFormBinded.apply }
    { MTagsAddFormBinded.unapply }
  }

  def addTagsForm: TagsEditForm_t = {
    Form(addTagsFormM)
  }

}


/** Интерфейс для DI-поля, дающего экземпляр [[TagsEditFormUtil]]. */
trait ITagsEditFormUtilDi {
  def tagsEditFormUtil: TagsEditFormUtil
}
