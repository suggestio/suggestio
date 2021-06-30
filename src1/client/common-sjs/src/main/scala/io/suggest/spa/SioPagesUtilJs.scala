package io.suggest.spa

import io.suggest.geo.MGeoPoint
import io.suggest.log.Log
import io.suggest.sc.ScConstants.ScJsState
import io.suggest.text.UrlUtilJs
import io.suggest.url.bind._
import japgolly.univeq._

object SioPagesUtilJs extends Log {

  /** JavaScript client-side URL QueryString bindable for SioPages.Sc3 (Showcase state URI query-string). */
  def sc3QsBindableF: QsBindable[SioPages.Sc3] = {
    import QsBindableUtilJs.{qsBindableInt, qsBindableLong, qsBindableBoolean}
    import QsbSeqUtil._

    implicit val stringB = QsBindableUtilJs.qsBindableString
    implicit val geoPointPipedB = MGeoPoint.qsBindablePiped

    SioPages.Sc3.sc3QsB
  }


  /** Parse Sc3 state from URL query string.
    *
    * @param qs Showcase URL QueryString.
    * @return Parsed Sc3 state.
    *         On errors, returns empty state.
    */
  def parseSc3FromQs(qs: String): SioPages.Sc3 = {
    val tokens = UrlUtilJs.qsParseToMap( qs )
    parseSc3FromQsTokens( tokens )
  }
  def parseSc3FromQsTokens( tokens: Map[String, Seq[String]] ): SioPages.Sc3 = {
    sc3QsBindableF
      .bindF( ScJsState.QSB_KEY, tokens )
      .flatMap( _.toOption )
      .getOrElse( SioPages.Sc3.empty )
  }


  /** Serialize Sc3-route back into queryString.
    *
    * @param mainScreen Showcase state.
    * @return URL query string.
    */
  def sc3ToQs(mainScreen: SioPages.Sc3): String =
    sc3QsBindableF.unbindF( ScJsState.QSB_KEY, mainScreen )

}
