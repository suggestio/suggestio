package io.suggest.sc.model.inx

import io.suggest.sc.index.MSc3IndexResp
import io.suggest.sc.sc3.MScQs
import japgolly.univeq.UnivEq
import monocle.macros.GenLens


/** Static companion for client-side index responses. */
object MIndexRespData {

  @inline implicit def univEq: UnivEq[MIndexRespData] = UnivEq.derive

  def resp = GenLens[MIndexRespData](_.resp)
  def scQs = GenLens[MIndexRespData](_.scQs)

}


/** Client-side data container for index response data.
  *
  * @param resp Server response.
  * @param scQs Query string args for request.
  */
final case class MIndexRespData(
                                 resp       : MSc3IndexResp,
                                 scQs       : MScQs,
                               )
