package io.suggest.sc.ssr

import io.suggest.i18n.MLanguage
import io.suggest.sc.sc3.{IScRespAction, MSc3Resp, MScQs}
import io.suggest.spa.DAction
import io.suggest.text.StringUtil

sealed trait IScSsrAction extends DAction

/**
  * @param scQs Query string info for request.
  * @param scResp Server responce for that.
  */
final case class SsrSetState(
                              scQs    : MScQs,
                              scResp  : MSc3Resp,
                              lang    : Option[SsrLangData] = None,
                            )
  extends IScRespAction
  with IScSsrAction


/** Language data to switch UI language on render. */
final case class SsrLangData(
                              lang           : MLanguage,
                              messagesMap    : Map[String, String],
                            ) {
  override def toString: String = {
    StringUtil.toStringHelper( this, 32 ) { renderF =>
      val noName = renderF("")
      noName( lang )
      noName( messagesMap.size.toString + " msgs" )
    }
  }
}