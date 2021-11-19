package io.suggest.sc.ssr

import io.suggest.sc.sc3.{IScRespAction, MSc3Resp, MScQs}
import io.suggest.spa.DAction

sealed trait IScSsrAction extends DAction

/**
  * @param scQs Query string info for request.
  * @param scResp Server responce for that.
  */
final case class SsrSetState(
                              scQs    : MScQs,
                              scResp  : MSc3Resp,
                            )
  extends IScRespAction
  with IScSsrAction
