package io.suggest.adn.edit.c

import diode._
import io.suggest.adn.edit.api.ILkAdnEditApi
import io.suggest.adn.edit.m.{MAdnEditErrors, MAdnEditPopups, MLkAdnEditRoot, SaveResp}
import io.suggest.lk.m.{CloseAllPopups, DocBodyClick, HandleNewHistogramInstalled, Save}
import io.suggest.msg.WarnMsgs
import io.suggest.primo.id.IId
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.04.18 21:35
  * Description: Контроллер внутренних фунцийи редактора ADN-узла.
  */
class RootAh[M](
                 api        : ILkAdnEditApi,
                 modelRW    : ModelRW[M, MLkAdnEditRoot]
               )
  extends ActionHandler( modelRW )
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Реакция на кнопку сохранения формы.
    case m @ Save =>
      val v0 = value
      if (v0.internals.saving.isPending) {
        LOG.log( WarnMsgs.REQUEST_STILL_IN_PROGRESS, msg = m )
        noChange
      } else {
        val fx = Effect {
          api
            .save( v0.toForm )
            .transform { tryRes =>
              Success( SaveResp(tryRes) )
            }
        }

        val v2 = v0.withInternals(
          v0.internals.withSaving(
            v0.internals.saving
              .pending()
          )
        )

        updated(v2, fx)
      }


    // Результат запроса сохранения.
    case m: SaveResp =>
      val v0 = value
      m.tryResp.fold(
        // Ошибка при запросе или на сервере.
        {ex =>
          val v2 = v0.withInternals(
            v0.internals.withSaving(
              v0.internals.saving
                .fail(ex)
            )
          )
          updated(v2)
        },
        // Положительный ответ
        {form2 =>
          // Залить данные с сервера в форму.
          val v2 = v0.copy(
            internals = v0.internals.withSaving(
              saving = v0.internals.saving
                .ready(form2)
            ),
            node = {
              // Для ускорения и упрощения: Новые эджи не заливаем (в них нет fileSrv), а просто фильтруем существующие эджи по edge uid.
              val newEdgeIds = IId.els2idsSet( form2.edges )
              v0.node.copy(
                meta    = form2.meta,
                edges   = v0.node.edges.filterKeys( newEdgeIds.contains ),
                resView = form2.resView,
                errors  = MAdnEditErrors.empty
              )
            }
          )
          updated( v2 )
        }
      )

    // Перехват ненужных событий.
    case _: HandleNewHistogramInstalled =>
      noChange

    // Закрытие всех попапов.
    case CloseAllPopups =>
      val v0 = value
      val v2 = v0.withPopups(
        MAdnEditPopups.empty
      )
      updated( v2 )

    // DocBodyClick используется для сокрытия color picker'а
    case DocBodyClick =>
      noChange

  }

}
