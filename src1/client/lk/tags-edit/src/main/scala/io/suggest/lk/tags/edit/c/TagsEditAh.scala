package io.suggest.lk.tags.edit.c

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.common.coll.SetUtil
import io.suggest.common.tags.TagFacesUtil
import io.suggest.common.tags.edit.{MTagsEditQueryProps, TagsEditConstants}
import io.suggest.i18n.MMessage
import io.suggest.lk.tags.edit.m._

import scala.concurrent.duration._
import diode.Implicits.runAfterImpl
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.log.Log
import io.suggest.tags.{ITagsApi, MTagsSearchQs}
import play.api.libs.json.Json

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 19:27
  * Description: Action handler для подсистемы редактора тегов на базе react+diode.
  */
class TagsEditAh[M](
                     modelRW         : ModelRW[M, MTagsEditState],
                     api             : ITagsApi,
                     priceUpdateFx   : Effect
)
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Выбор тега среди найденных: добавить в exists-теги.
    case AddTagFound(tagFace) =>
      val v0 = value

      val tagFaces = TagFacesUtil.query2tags(tagFace)
      // Если после добавления тега (тегов) множество тегов не изменилось внутри, то поддерживаем исходную референсную целостность.
      val te2 = SetUtil.addToSetOrKeepRef1(v0.props.tagsExists, tagFaces)

      // Собрать и сохранить новое состояние редактора тегов, сбросив поисковое поле.
      val p2 = v0.props.copy(
        query       = MTagsEditQueryProps(),
        tagsExists  = te2
      )

      // И надо забыть обо всех найденных тегах:
      val v2 = v0.reset.withProps(p2)
      updated( v2, priceUpdateFx )


    // Замена текста поискового запроса тегов.
    case SetTagSearchQuery(q) =>
      val v0 = value
      // Сначала подготовить пропертисы...
      val p0 = v0.props

      val text0 = p0.query.text
      // Проверяем тут, изменился ли текст на самом деле:
      val v1 = if (q != text0) {
        v0.withProps(
          p0.withQuery(
            p0.query.withText( q )
          )
        )
      } else {
        v0
      }

      // Затем запланировать поисковый запрос к серверу, если необходимо.
      val qtrim = q.trim
      if (qtrim.isEmpty) {
        // Пусто в поисковом запросе. Сбросить состояние поиска.
        updated( v1.reset )

      } else if (qtrim == text0) {
        // Текст вроде бы не изменился относительно предыдущего шага.
        updated( v1 )

      } else {
        // Запланировать эффект запуска запроса с помощью эффекта ожидания.
        val now = System.currentTimeMillis()
        val awaitFx = StartSearchReq(now).toEffectPure
          .after( TagsEditConstants.Search.START_SEARCH_TIMER_MS.milliseconds )

        // Залить в состояние итоги запуска запроса:
        val v2 = v1.withSearchTimer( Some(now) )

        updated(v2, awaitFx)
      }


    // Настала пора запуска реквеста
    case StartSearchReq(now0) =>
      val v0 = value
      if ( v0.searchTimer.contains(now0) ) {
        // Можно начинать искать теги на сервере...
        val fx = Effect( startTagsSearch(now0) )
        val v1 = v0.withFound( v0.found.pending() )
        updated( v1, fx )

      } else {
        // Данный эффект поиска устарел, не успев начаться.
        // Возможно запущен какой-то другой эффект или он просто более не требуется.
        noChange
      }


    // Среагировать на ответ сервера по поводу поиска тегов.
    case HandleTagsFound(resp, now0) =>
      val v0 = value
      if (v0.searchTimer.contains(now0)) {
        // Это ожидаемый запрос, обновить состояние.
        val v1 = v0.copy(
          found       = v0.found.ready(resp),
          searchTimer = None
        )
        updated( v1 )

      } else {
        // Этот ответ не является ожидаемым. Скорее всего, он устарел или race-conditions на линии
        noChange
      }


    // Добавление текущего введённого тега в список текущих тегов.
    case AddCurrentTag =>
      val v0 = value
      val p0 = v0.props
      // Если поле пустое или слишком короткое, то красным его подсветить.
      // А если есть вбитый тег, то огранизовать добавление.
      val faces = TagFacesUtil.query2tags( p0.query.text )

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
            // Слишком короткое имя тега:
            MMessage("error.minLength", Json.arr(minLen) ) :: Nil
          } else if (ql > maxLen) {
            // Максимальную длина превышена:
            MMessage("error.maxLength", Json.arr(maxLen) ) :: Nil
          } else {
            Nil
          }
        }
      }

      if (errors.isEmpty) {
        // Ошибок валидации нет. Заливаем в старое множество новые теги...
        val te2 = SetUtil.addToSetOrKeepRef1(p0.tagsExists, faces)
        val p2 = p0.copy(
          query       = MTagsEditQueryProps(),
          tagsExists  = te2
        )
        val v2 = v0.withProps(p2)
        // Если что-то реально изменилось (te2 != te0), то запустить эффект пересчёта стоимости.

        if (te2 != p0.tagsExists) {
          updated(v2, priceUpdateFx)
        } else {
          updated(v2)
        }
      } else {
        // Есть хотя бы одна ошибка. Закинуть ошибки в состояние.
        val p2 = p0.withQuery( p0.query.withErrors(errors) )
        updated( v0.withProps(p2) )
      }


    // Удаление тега из списка добавленных ранее (existing) тегов.
    case RmTag(tagFace) =>
      val v0 = value
      val p0 = v0.props
      val te0 = p0.tagsExists
      val te1 = te0 - tagFace
      if (te1.size < te0.size) {
        val p2 = p0.withTagsExists(te1)
        val v2 = v0.withProps(p2)
        updated(v2, priceUpdateFx)
      } else {
        noChange
      }

  }



  /** Код эффекта запроса поиска тегов. */
  def startTagsSearch(now: Long): Future[HandleTagsFound] = {
    val qsArgs = MTagsSearchQs(
      faceFts = value.props.query.text,
      limit   = Some( TagsEditConstants.Search.LIVE_SEARCH_RESULTS_LIMIT )
    )
    for (resp <- api.tagsSearch(qsArgs)) yield {
      HandleTagsFound(resp, now)
    }
  }


}
