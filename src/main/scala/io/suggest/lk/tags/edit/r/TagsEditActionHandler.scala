package io.suggest.lk.tags.edit.r

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.common.coll.SetUtil
import io.suggest.common.tags.TagFacesUtil
import io.suggest.common.tags.edit.{MTagsEditS, MTagsSearchS, TagsEditConstants}
import io.suggest.i18n.MMessage

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 19:27
  * Description: Action handler для подсистемы редактора тегов на базе react+diode.
  */
class TagsEditActionHandler[M](
  modelRW     : ModelRW[M, MTagsEditS]
)
  extends ActionHandler(modelRW) {

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Выбор тега среди найденных: добавить в exists-теги.
    case AddTagFound(tagFace) =>
      val v0 = value
      val tagFaces = TagFacesUtil.query2tags(tagFace)
      // Если после добавления тега (тегов) множество тегов не изменилось внутри, то поддерживаем исходную референсную целостность.
      val te2 = SetUtil.addToSetOrKeepRef1(v0.tagsExists, tagFaces)

      // Собрать и сохранить новое состояние редактора тегов, сбросив поисковое поле.
      val v1 = v0.copy(
        query       = MTagsSearchS(),
        tagsExists  = te2
      )
      // TODO Нужно запускать эффект пересчёта стоимости, а точнее дёргать какой-то соответствующий callback.
      updated(v1)


    // Замена текста поискового запроса тегов.
    case SetTagSearchQuery(q) =>
      val v0 = value
      val v1 = v0.withQuery(
        v0.query.withText( q )
      )
      // TODO Нужен эффект запуска поискового запроса. RPC или Route передавать сюда через конструктор этого handler'а.
      updated(v1)


    // Добавление текущего введённого тега в список текущих тегов.
    case AddCurrentTag =>
      val v = value
      // Если поле пустое или слишком короткое, то красным его подсветить.
      // А если есть вбитый тег, то огранизовать добавление.
      val faces = TagFacesUtil.query2tags( v.query.text )

      // Проверяем название тега...
      val errors: Seq[MMessage] = if (faces.isEmpty) {
        MMessage( "error.required" ) :: Nil
      } else {
        // Проверяем минимальную длину
        val minLen = TagsEditConstants.Constraints.TAG_LEN_MIN
        val maxLen = TagsEditConstants.Constraints.TAG_LEN_MAX
        faces.flatMap { q =>
          val ql = q.length
          if (ql < minLen) {
            MMessage.a("error.minLength", minLen) :: Nil
          } else {
            // Проверяем максимальную длину.
            if (ql > maxLen) {
              MMessage.a("error.maxLength", maxLen) :: Nil
            } else {
              Nil
            }
          }
        }
      }

      val v2 = if (errors.isEmpty) {
        // Ошибок валидации нет. Заливаем в старое множество новые теги...
        val te2 = SetUtil.addToSetOrKeepRef1(v.tagsExists, faces)
        // TODO Если te2 != te0, то запустить эффект пересчёта стоимости.
        v.copy(
          query       = MTagsSearchS(),
          tagsExists  = te2
        )
      } else {
        // Есть хотя бы одна ошибка. Закинуть ошибки в состояние.
        v.withQuery(
          v.query.withErrors( errors ))
      }
      updated(v2)


    // Удаление тега из списка добавленных ранее (existing) тегов.
    case RmTag(tagFace) =>
      val v = value
      val te0 = v.tagsExists
      val te1 = te0 - tagFace
      if (te1.size < te0.size) {
        // TODO Запустить пересчёт стоимости размещения.
        updated(v.withTagsExists(te1))
      } else {
        noChange
      }

  }

}
