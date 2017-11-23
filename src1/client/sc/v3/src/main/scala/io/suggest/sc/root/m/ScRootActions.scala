package io.suggest.sc.root.m

import io.suggest.routes.scRoutes
import io.suggest.sc.sc3.MSc3Resp
import io.suggest.spa.DAction

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.07.17 18:30
  * Description: Корневые экшены sc3.
  */

/** Интерфейс корневых экшенов. */
sealed trait IScRootAction extends DAction

/** Сигнал основной цепочке о состоянии основного js-роутера. */
case class JsRouterStatus( payload: Try[scRoutes.type] ) extends IScRootAction

/** Получен какой-то ответ сервера по поводу индекса выдачи.
  *
  * @param reqTimestamp Таймштамп index-запроса.
  *                     Может быть None, чтобы форсировать обновление выдачи без учёта timestamp'а запроса.
  */
case class HandleIndexResp(tryResp: Try[MSc3Resp], reqTimestamp: Option[Long]) extends IScRootAction
