package io.suggest.sc.inx.m

import io.suggest.sc.m.ISc3Action
import io.suggest.sc.resp.MSc3Resp

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.07.17 18:41
  * Description: Diode-экшены для index'а.
  */

/** Интерфейс-маркер для index-экшенов. */
sealed trait IIndexAction extends ISc3Action

/** Дёрнуть индекс с сервера и накатить. */
case class GetIndex(rcvrId: Option[String]) extends IIndexAction

/** Ответ сервера на запрос */
case class HandleScResp(timestampMs: Long, tryResp: Try[MSc3Resp]) extends IIndexAction
