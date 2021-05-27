package io.suggest.es.model

import japgolly.univeq._
import monocle.macros.GenLens
import org.elasticsearch.index.seqno.SequenceNumbers

object EsDocVersion {

  lazy val empty = EsDocVersion()

  def fromRawValues(version: Long, seqNo: Long, primaryTerm: Long): EsDocVersion = {
    EsDocVersion(
      version       = Option.when( version >= 0L )(version),
      seqNo         = Option.when( seqNo > SequenceNumbers.UNASSIGNED_SEQ_NO )(seqNo),
      primaryTerm   = Option.when( primaryTerm > SequenceNumbers.UNASSIGNED_PRIMARY_TERM )(primaryTerm),
    )
  }

  def notSaveable: EsDocVersion =
    apply( version = Some(-1) )


  @inline implicit def univEq: UnivEq[EsDocVersion] = UnivEq.derive

  def version = GenLens[EsDocVersion](_.version)
  def seqNo = GenLens[EsDocVersion](_.seqNo)
  def primaryTerm = GenLens[EsDocVersion](_.primaryTerm)

}


/** Elasticsearch document versions container.
  *
  * @param version Internal versioning.
  * @param seqNo Shard sequence number.
  * @param primaryTerm Shard primary term.
  */
final case class EsDocVersion(
                               version          : Option[Long]          = None,
                               seqNo            : Option[Long]          = None,
                               primaryTerm      : Option[Long]          = None,
                             )
