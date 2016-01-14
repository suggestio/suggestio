package controllers.ctag

import controllers.SioController
import io.suggest.model.n2.extra.tag.search.{ITagFaceCriteria, MTagFaceCriteria}
import io.suggest.model.n2.node.MNode
import models.mtag.{MTagsAddFormBinded, MTagBinded, MTagSearch, MAddTagReplyOk}
import play.api.libs.json.Json
import util.PlayMacroLogsI
import util.acl.IsAuth
import util.tags.ITagsEditFormUtilDi
import views.html.lk.tag.edit._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 16:45
 * Description: Аддон для контроллера редактора карточки (узла), добавляющий экшены поддержки тегов.
 */

trait NodeTagsEdit
  extends SioController
  with PlayMacroLogsI
  with IsAuth
  with ITagsEditFormUtilDi
{

  import mCommonDi._

  /**
   * Добавление тега в редактор тегов.
   * Этот экшен не трогает никакие текущие данные в моделях,
   * а просто анализирует теги, добавляет новый, сортирует и рендерит.
   */
  def tagEditorAddTag = IsAuth.async { implicit request =>
    val formBinded = tagsEditFormUtil.addTagsForm.bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        LOGGER.debug("tagEditorAddTag(): Form bind failed:\n" + formatFormErrors(formWithErrors))
        val formHtml = _addFormTpl(formWithErrors)
        NotAcceptable( formHtml )
      },

      // Удачный маппинг запроса на форму.
      {r =>
        // Запустить поиск подходящих тегов под добавленные.
        val foundFut = Future.traverse(r.added) { newTagFace =>
          val msearch = new MTagSearch {
            override def tagFaces: Seq[ITagFaceCriteria] = {
              val cr = MTagFaceCriteria(newTagFace, isPrefix = false)
              Seq(cr)
            }
            override def limit = 1
          }
          MNode.dynSearchOne(msearch)
            .map { newTagFace -> _ }
        }

        // Подготовить карту уже существующих тегов.
        val existMap = r.existing
          .iterator
          .map { t => t.face -> t }
          .toMap

        // Собрать и вернуть асинхронный результат.
        for {
          found <- foundFut
        } yield {

          // Привести теги к MTagBinded
          val existsMap2 = {
            val mtesIter = for ((face, mnodeOpt) <- found.iterator) yield {
              val mte = MTagBinded(
                face    = mnodeOpt
                  .flatMap(_.extras.tag)
                  .flatMap(_.faces.headOption)
                  .fold(face)(_._1),
                nodeId  = mnodeOpt.flatMap(_.id)
              )
              mte.face -> mte
            }
            existMap ++ mtesIter
          }

          // Собрать новый маппинг формы для рендера.
          val tf2 = {
            val r2 = MTagsAddFormBinded(
              added     = Nil,
              existing  = existsMap2.valuesIterator.toList
            )
            formBinded.fill(r2)
          }

          // Собрать JSON-ответ и вернуть.
          val resp = MAddTagReplyOk(
            addFormHtml = _addFormTpl  (tf2),
            existHtml   = _tagsExistTpl(tf2)
          )

          // Отрендерить и вернуть результат.
          Ok( Json.toJson(resp) )
            .withHeaders(CACHE_CONTROL -> "no-cache")
        }
      }
    )
  }

}
