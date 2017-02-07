package controllers.ctag

import akka.util.ByteString
import controllers.SioController
import io.suggest.common.tags.search.MTagsFound.pickler
import io.suggest.model.n2.tag.MTagSearchResp
import io.suggest.pick.PickleUtil
import io.suggest.util.logs.IMacroLogs
import models.mctx.Context
import models.mlk.MLkTagsSearchQs
import models.mtag._
import play.api.libs.json.Json
import util.acl.IIsAuth
import util.lk.ITagSearchUtilDi
import util.tags.ITagsEditFormUtilDi
import views.html.lk.tag.edit._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 16:45
 * Description: Аддон для контроллера редактора карточки (узла), добавляющий экшены поддержки тегов.
 */

trait NodeTagsEdit
  extends SioController
  with IMacroLogs
  with IIsAuth
  with ITagsEditFormUtilDi
  with ITagSearchUtilDi
{

  import mCommonDi._

  /**
   * Добавление тега в редактор тегов.
   * Этот экшен не трогает никакие текущие данные в моделях,
   * а просто анализирует теги, добавляет новый, сортирует и рендерит.
   */
  def tagEditorAddTag = isAuth() { implicit request =>
    val formBinded = tagsEditFormUtil.addTagsForm.bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        LOGGER.debug("tagEditorAddTag(): Form bind failed:\n" + formatFormErrors(formWithErrors))
        val formHtml = _addFormTpl(formWithErrors)
        NotAcceptable( formHtml )
      },

      {r =>
        // Собрать новый набор добавленных тегов.
        val existing2 = {
          val existingAcc = List.newBuilder[MTagBinded]
          existingAcc ++= r.existing
          existingAcc ++= {
            for (tf <- r.added.iterator) yield {
              MTagBinded(tf)
            }
          }
          existingAcc.result()
        }

        implicit val ctx = implicitly[Context]

        // Собрать обновлённый маппинг формы:
        val r2 = MTagsAddFormBinded(
          added     = Nil,
          existing  = existing2
        )
        val tf2 = formBinded.fill(r2)

        // Собрать JSON-ответ и вернуть.
        val resp = MAddTagReplyOk(
          addFormHtml = htmlCompressUtil.html2str4json( _addFormTpl(tf2)(ctx) ),
          existHtml   = htmlCompressUtil.html2str4json( _tagsExistTpl(tf2)(ctx) )
        )

        // Отрендерить и вернуть результат.
        Ok( Json.toJson(resp) )
          .withHeaders(CACHE_CONTROL -> "no-cache")
      }
    )
  }

  /** Экшен поиска похожих тегов во время набора названия.
    *
    * @return JSON с inline-версткой для отображения в качестве выпадающего списка.
    */
  // TODO Кажется, что это можно удалить вместе со без-react'ной формой lk-adv-geo.
  def tagsSearch(tsearch: MLkTagsSearchQs) = isAuth().async { implicit request =>
    for {
      found <-  tagSearchUtil.liveSearchTagsFromQs( tsearch )
    } yield {
      if (found.tags.isEmpty) {
        NoContent
      } else {
        val resp = MTagSearchResp(
          rendered    = Some( htmlCompressUtil.html2str4json(_tagsHintTpl(found)) ),
          foundCount  = found.tags.size
        )
        Ok( Json.toJson(resp) )
      }
    }
  }


  /** Поиск тегов по полубинарному протоколу (ответ бинарный).
    *
    * @param tsearch query string.
    * @return Сериализованная модель MTagsFound.
    */
  def tagsSearch2(tsearch: MLkTagsSearchQs) = isAuth().async { implicit request =>
    for {
      found <-  tagSearchUtil.liveSearchTagsFromQs( tsearch )
    } yield {
      LOGGER.trace(s"tagSearch2(${request.rawQueryString}): Found ${found.tags.size} tags: ${found.tags.iterator.map(_.face).mkString(", ")}")
      Ok( ByteString(PickleUtil.pickle(found)) )
    }
  }

}
